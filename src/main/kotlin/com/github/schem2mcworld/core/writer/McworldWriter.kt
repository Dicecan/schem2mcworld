package com.github.schem2mcworld.core.writer

import com.github.schem2mcworld.api.ConversionOptions
import com.github.schem2mcworld.api.ConversionResult
import com.github.schem2mcworld.core.mapper.BlockStateMapper
import com.github.schem2mcworld.core.mapper.FallbackPolicy
import com.github.schem2mcworld.core.mapper.MappingDataLoader
import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.model.BlockEntity
import com.github.schem2mcworld.core.model.SchematicData
import com.github.schem2mcworld.core.terrain.CoordinateTransformer
import com.github.schem2mcworld.core.terrain.WorldTerrainGenerator
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class McworldWriter(
    private val templateManager: WorldTemplateManager = WorldTemplateManager()
) {
    fun write(
        schematic: SchematicData,
        options: ConversionOptions,
        destinationFile: File
    ): ConversionResult {
        destinationFile.parentFile?.mkdirs()
        return destinationFile.outputStream().use { fos ->
            writeToStream(schematic, options, fos, destinationFile.length())
        }
    }

    fun writeToStream(
        schematic: SchematicData,
        options: ConversionOptions,
        outputStream: OutputStream,
        knownFileSize: Long = 0
    ): ConversionResult {
        val startTime = System.currentTimeMillis()

        val terrain = WorldTerrainGenerator(
            terrainType = options.terrainType,
            customLayers = options.customLayers,
            targetVersion = options.targetVersion
        )
        val bounds = CoordinateTransformer.calculatePlacement(
            schematic = schematic,
            targetPosition = options.targetPosition,
            alignment = options.alignment,
            terrain = terrain,
            targetVersion = options.targetVersion
        )

        val mapper = options.customMapper ?: run {
            val fallback = if (options.fallbackBlock != BedrockBlockState.STONE) {
                FallbackPolicy.of(options.fallbackBlock)
            } else {
                FallbackPolicy.SMART
            }
            BlockStateMapper(
                rules = MappingDataLoader.loadDefaultJavaToBedrock(),
                fallbackPolicy = fallback,
                modFilterPolicy = options.modFilterPolicy
            )
        }

        val tempDir = Files.createTempDirectory("mcworld_builder_").toFile()
        var chunksCount = 0
        var subChunksCount = 0

        try {
            val dbDir = File(tempDir, "db")
            dbDir.mkdirs()

            val dbOptions = Options().apply { createIfMissing(true) }
            val db = Iq80DBFactory.factory.open(dbDir, dbOptions)

            try {
                val blockEntityMap = HashMap<Pair<Int, Int>, MutableList<BlockEntity>>()
                for (be in schematic.blockEntities) {
                    val absPos = bounds.origin + be.position
                    val chunkX = absPos.x shr 4
                    val chunkZ = absPos.z shr 4
                    val chunkKey = Pair(chunkX, chunkZ)
                    blockEntityMap.computeIfAbsent(chunkKey) { mutableListOf() }.add(be)
                }

                for (chunkX in bounds.minChunkX..bounds.maxChunkX) {
                    for (chunkZ in bounds.minChunkZ..bounds.maxChunkZ) {
                        chunksCount++

                        db.put(
                            LevelDbHelper.createChunkVersionKey(chunkX, chunkZ),
                            byteArrayOf(LevelDbHelper.OVERWORLD_VERSION_VALUE)
                        )

                        db.put(
                            LevelDbHelper.create2DDataKey(chunkX, chunkZ),
                            LevelDbHelper.createDefault2DData(terrain.surfaceY.toShort())
                        )

                        val entitiesInChunk = blockEntityMap[Pair(chunkX, chunkZ)]
                        if (!entitiesInChunk.isNullOrEmpty()) {
                            db.put(
                                LevelDbHelper.createBlockEntityKey(chunkX, chunkZ),
                                LevelDbHelper.serializeBlockEntities(entitiesInChunk)
                            )
                        }

                        for (subChunkY in bounds.minSubChunkY..bounds.maxSubChunkY) {
                            val subBlocks = Array(SubChunkEncoder.SUBCHUNK_SIZE) { BedrockBlockState.AIR }
                            var hasNonAir = false

                            for (lx in 0 until 16) {
                                for (lz in 0 until 16) {
                                    for (ly in 0 until 16) {
                                        val worldX = (chunkX shl 4) + lx
                                        val worldY = (subChunkY shl 4) + ly
                                        val worldZ = (chunkZ shl 4) + lz
                                        val subIndex = SubChunkEncoder.getLocalIndex(lx, ly, lz)

                                        if (bounds.contains(worldX, worldY, worldZ)) {
                                            val local = bounds.toLocalPos(worldX, worldY, worldZ)
                                            val uBlock = schematic.getBlock(local.x, local.y, local.z)

                                            if (uBlock.id != "air" && uBlock.id != "cave_air" && uBlock.id != "void_air") {
                                                val bedrockState = mapper.map(uBlock, options.targetVersion)
                                                subBlocks[subIndex] = bedrockState
                                                hasNonAir = true
                                            } else {
                                                val terrainBlock = terrain.getBlockAtY(worldY)
                                                if (terrainBlock != null) {
                                                    subBlocks[subIndex] = terrainBlock
                                                    hasNonAir = true
                                                }
                                            }
                                        } else {
                                            val terrainBlock = terrain.getBlockAtY(worldY)
                                            if (terrainBlock != null) {
                                                subBlocks[subIndex] = terrainBlock
                                                hasNonAir = true
                                            }
                                        }
                                    }
                                }
                            }

                            if (hasNonAir || terrain.subChunkIntersectsTerrain(subChunkY)) {
                                val encoded = SubChunkEncoder.encode(subBlocks, options.targetVersion.storageVersion)
                                val subChunkKey = LevelDbHelper.createSubChunkKey(chunkX, chunkZ, subChunkY)
                                db.put(subChunkKey, encoded)
                                subChunksCount++
                            }
                        }
                    }
                }
            } finally {
                db.close()
            }

            templateManager.writeLevelDat(
                file = File(tempDir, "level.dat"),
                worldName = options.worldName,
                spawnX = bounds.origin.x + schematic.width / 2,
                spawnY = bounds.origin.y + schematic.height,
                spawnZ = bounds.origin.z + schematic.length / 2,
                terrain = terrain,
                targetVersion = options.targetVersion
            )
            templateManager.writeLevelName(File(tempDir, "levelname.txt"), options.worldName)
            templateManager.writeWorldIcon(File(tempDir, "world_icon.jpeg"))

            zipDirectory(tempDir, outputStream)

        } finally {
            tempDir.deleteRecursively()
        }

        val durationMs = System.currentTimeMillis() - startTime

        return ConversionResult(
            success = true,
            worldName = options.worldName,
            totalBlocksConverted = schematic.totalBlocks.toLong(),
            chunksGenerated = chunksCount,
            subChunksWritten = subChunksCount,
            durationMs = durationMs,
            unmappedBlocks = mapper.getUnmappedBlocks(),
            outputSizeBytes = knownFileSize
        )
    }

    private fun zipDirectory(sourceDir: File, outputStream: OutputStream) {
        val zipOut = ZipOutputStream(outputStream)
        val sourcePath = sourceDir.toPath()

        sourceDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val relativePath = sourcePath.relativize(file.toPath()).toString().replace('\\', '/')
            val entry = ZipEntry(relativePath)
            zipOut.putNextEntry(entry)
            FileInputStream(file).use { it.copyTo(zipOut) }
            zipOut.closeEntry()
        }
        zipOut.finish()
    }
}
