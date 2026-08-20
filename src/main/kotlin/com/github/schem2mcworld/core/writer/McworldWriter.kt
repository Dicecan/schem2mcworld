package com.github.schem2mcworld.core.writer

import com.github.schem2mcworld.api.ConversionOptions
import com.github.schem2mcworld.api.ConversionResult
import com.github.schem2mcworld.api.SchematicPlacement
import com.github.schem2mcworld.core.mapper.BlockStateMapper
import com.github.schem2mcworld.core.mapper.FallbackPolicy
import com.github.schem2mcworld.core.mapper.MappingDataLoader
import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.model.BlockEntity
import com.github.schem2mcworld.core.model.SchematicData
import com.github.schem2mcworld.core.parser.ParserFactory
import com.github.schem2mcworld.core.terrain.CoordinateTransformer
import com.github.schem2mcworld.core.terrain.WorldPlacementBounds
import com.github.schem2mcworld.core.terrain.WorldTerrainGenerator
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class McworldWriter(
    private val templateManager: WorldTemplateManager = WorldTemplateManager()
) {
    fun write(
        schematic: SchematicData?,
        options: ConversionOptions,
        destinationFile: File
    ): ConversionResult {
        destinationFile.parentFile?.mkdirs()
        val tempOutput = File.createTempFile("mcworld_dest_", ".tmp")
        try {
            val result = tempOutput.outputStream().use { fos ->
                writeToStream(schematic, options, fos)
            }
            tempOutput.copyTo(destinationFile, overwrite = true)
            return result.copy(outputSizeBytes = destinationFile.length())
        } finally {
            tempOutput.delete()
        }
    }

    fun writeToStream(
        schematic: SchematicData?,
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

        val rawPlacements = if (options.placements.isNotEmpty()) {
            options.placements
        } else if (schematic != null) {
            listOf(SchematicPlacement(schematic, options.targetPosition, options.alignment, options.pasteAir))
        } else {
            emptyList()
        }

        val loadedPlacements = mutableListOf<Pair<SchematicData, WorldPlacementBounds>>()
        var totalBlocksSum = 0L

        for (p in rawPlacements) {
            val sData: SchematicData = when (val src = p.source) {
                is SchematicData -> src
                is File -> ParserFactory.parse(src)
                is InputStream -> ParserFactory.parse(src)
                else -> throw IllegalArgumentException("Unsupported schematic source: ${src.javaClass.name}")
            }
            val bounds = CoordinateTransformer.calculatePlacement(
                schematic = sData,
                targetPosition = p.position,
                alignment = p.alignment,
                terrain = terrain,
                targetVersion = options.targetVersion
            )
            loadedPlacements.add(Pair(sData, bounds))
            totalBlocksSum += sData.totalBlocks
        }

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
            val baseWorld = options.baseWorld
            if (baseWorld != null && baseWorld.exists()) {
                if (baseWorld.isFile) {
                    unzip(baseWorld, tempDir)
                } else if (baseWorld.isDirectory) {
                    baseWorld.copyRecursively(tempDir, overwrite = true)
                }
            }

            val dbDir = File(tempDir, "db")
            dbDir.mkdirs()

            val dbOptions = Options().apply { createIfMissing(true) }
            val db = Iq80DBFactory.factory.open(dbDir, dbOptions)

            try {
                // 1. 收集 Chunk 范围
                val chunkCoords = mutableSetOf<Pair<Int, Int>>()
                if (baseWorld == null && terrain.effectiveLayers.isNotEmpty()) {
                    for (pl in loadedPlacements) {
                        for (cx in pl.second.minChunkX..pl.second.maxChunkX) {
                            for (cz in pl.second.minChunkZ..pl.second.maxChunkZ) {
                                chunkCoords.add(Pair(cx, cz))
                            }
                        }
                    }
                } else {
                    for (pl in loadedPlacements) {
                        for (cx in pl.second.minChunkX..pl.second.maxChunkX) {
                            for (cz in pl.second.minChunkZ..pl.second.maxChunkZ) {
                                chunkCoords.add(Pair(cx, cz))
                            }
                        }
                    }
                }

                // 2. 收集方块实体
                val blockEntityMap = HashMap<Pair<Int, Int>, MutableList<BlockEntity>>()
                for (pl in loadedPlacements) {
                    val sData = pl.first
                    val bounds = pl.second
                    for (be in sData.blockEntities) {
                        val absPos = bounds.origin + be.position
                        val chunkX = absPos.x shr 4
                        val chunkZ = absPos.z shr 4
                        val chunkKey = Pair(chunkX, chunkZ)
                        blockEntityMap.computeIfAbsent(chunkKey) { mutableListOf() }.add(be)
                    }
                }

                // 3. 逐区块逐子区块覆盖写入
                for ((chunkX, chunkZ) in chunkCoords) {
                    chunksCount++

                    val vKey = LevelDbHelper.createChunkVersionKey(chunkX, chunkZ)
                    if (db.get(vKey) == null) {
                        db.put(vKey, byteArrayOf(LevelDbHelper.OVERWORLD_VERSION_VALUE))
                    }

                    val d2Key = LevelDbHelper.create2DDataKey(chunkX, chunkZ)
                    if (db.get(d2Key) == null) {
                        db.put(d2Key, LevelDbHelper.createDefault2DData(terrain.surfaceY.toShort()))
                    }

                    val entitiesInChunk = blockEntityMap[Pair(chunkX, chunkZ)]
                    if (!entitiesInChunk.isNullOrEmpty()) {
                        db.put(
                            LevelDbHelper.createBlockEntityKey(chunkX, chunkZ),
                            LevelDbHelper.serializeBlockEntities(entitiesInChunk)
                        )
                    }

                    // 计算该 Chunk 所涉及的 SubChunk Y 范围
                    val relevantPlacements = loadedPlacements.filter {
                        chunkX in it.second.minChunkX..it.second.maxChunkX &&
                        chunkZ in it.second.minChunkZ..it.second.maxChunkZ
                    }

                    val minSubY = if (relevantPlacements.isNotEmpty()) {
                        relevantPlacements.minOf { it.second.minSubChunkY }
                    } else options.targetVersion.minSubChunkY

                    val maxSubY = if (relevantPlacements.isNotEmpty()) {
                        relevantPlacements.maxOf { it.second.maxSubChunkY }
                    } else options.targetVersion.maxSubChunkY

                    for (subChunkY in minSubY..maxSubY) {
                        val subChunkKey = LevelDbHelper.createSubChunkKey(chunkX, chunkZ, subChunkY)
                        val existingBytes = db.get(subChunkKey)
                        val subBlocks: Array<BedrockBlockState> = if (existingBytes != null) {
                            SubChunkEncoder.decode(existingBytes) ?: Array(SubChunkEncoder.SUBCHUNK_SIZE) { BedrockBlockState.AIR }
                        } else {
                            val arr = Array(SubChunkEncoder.SUBCHUNK_SIZE) { BedrockBlockState.AIR }
                            if (baseWorld == null && terrain.effectiveLayers.isNotEmpty()) {
                                for (lx in 0 until 16) {
                                    for (lz in 0 until 16) {
                                        for (ly in 0 until 16) {
                                            val worldY = (subChunkY shl 4) + ly
                                            val tBlock = terrain.getBlockAtY(worldY)
                                            if (tBlock != null) {
                                                arr[SubChunkEncoder.getLocalIndex(lx, ly, lz)] = tBlock
                                            }
                                        }
                                    }
                                }
                            }
                            arr
                        }

                        var hasModified = false

                        for (pl in relevantPlacements) {
                            val sData = pl.first
                            val bounds = pl.second

                            for (lx in 0 until 16) {
                                for (lz in 0 until 16) {
                                    for (ly in 0 until 16) {
                                        val worldX = (chunkX shl 4) + lx
                                        val worldY = (subChunkY shl 4) + ly
                                        val worldZ = (chunkZ shl 4) + lz

                                        if (bounds.contains(worldX, worldY, worldZ)) {
                                            val local = bounds.toLocalPos(worldX, worldY, worldZ)
                                            val uBlock = sData.getBlock(local.x, local.y, local.z)
                                            val subIndex = SubChunkEncoder.getLocalIndex(lx, ly, lz)

                                            if (uBlock.id != "air" && uBlock.id != "cave_air" && uBlock.id != "void_air") {
                                                val bedrockState = mapper.map(uBlock, options.targetVersion)
                                                subBlocks[subIndex] = bedrockState
                                                hasModified = true
                                            } else if (options.pasteAir) {
                                                subBlocks[subIndex] = BedrockBlockState.AIR
                                                hasModified = true
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (hasModified || existingBytes != null || (baseWorld == null && terrain.subChunkIntersectsTerrain(subChunkY))) {
                            val encoded = SubChunkEncoder.encode(subBlocks, options.targetVersion.storageVersion)
                            db.put(subChunkKey, encoded)
                            subChunksCount++
                        }
                    }
                }
            } finally {
                db.close()
            }

            if (baseWorld == null) {
                val spawnX = if (loadedPlacements.isNotEmpty()) loadedPlacements[0].second.origin.x + loadedPlacements[0].first.width / 2 else 0
                val spawnY = if (loadedPlacements.isNotEmpty()) loadedPlacements[0].second.origin.y + loadedPlacements[0].first.height else 64
                val spawnZ = if (loadedPlacements.isNotEmpty()) loadedPlacements[0].second.origin.z + loadedPlacements[0].first.length / 2 else 0

                templateManager.writeLevelDat(
                    file = File(tempDir, "level.dat"),
                    worldName = options.worldName,
                    spawnX = spawnX,
                    spawnY = spawnY,
                    spawnZ = spawnZ,
                    terrain = terrain,
                    targetVersion = options.targetVersion
                )
                templateManager.writeLevelName(File(tempDir, "levelname.txt"), options.worldName)
                templateManager.writeWorldIcon(File(tempDir, "world_icon.jpeg"))
            }

            zipDirectory(tempDir, outputStream)

        } finally {
            tempDir.deleteRecursively()
        }

        val durationMs = System.currentTimeMillis() - startTime

        return ConversionResult(
            success = true,
            worldName = options.worldName,
            totalBlocksConverted = totalBlocksSum,
            chunksGenerated = chunksCount,
            subChunksWritten = subChunksCount,
            durationMs = durationMs,
            unmappedBlocks = mapper.getUnmappedBlocks(),
            outputSizeBytes = knownFileSize
        )
    }

    private fun unzip(zipFile: File, destDir: File) {
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val entryDest = File(destDir, entry.name)
                if (entry.isDirectory) {
                    entryDest.mkdirs()
                } else {
                    entryDest.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        entryDest.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
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
