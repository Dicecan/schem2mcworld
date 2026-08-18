package com.github.schem2mcworld.api

import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.model.BedrockVersion
import com.github.schem2mcworld.core.util.LittleEndianNbtUtil
import com.github.schem2mcworld.core.util.VarIntUtil
import net.querz.nbt.io.NBTOutputStream
import net.querz.nbt.io.NamedTag
import net.querz.nbt.tag.ByteArrayTag
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.IntTag
import net.querz.nbt.tag.ShortTag
import net.querz.nbt.tag.StringTag
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class McworldConverterTest {

    @Test
    fun `test end-to-end convert sponge schem to mcworld with superflat terrain`(@TempDir tempDir: File) {
        // 1. 生成测试用 .schem 数据
        val schemFile = File(tempDir, "sample_building.schem")
        val root = CompoundTag()
        root.put("Width", ShortTag(3))
        root.put("Height", ShortTag(3))
        root.put("Length", ShortTag(3))
        root.put("Version", IntTag(2))
        root.put("DataVersion", IntTag(2586))

        val palette = CompoundTag()
        palette.put("minecraft:air", IntTag(0))
        palette.put("minecraft:stone", IntTag(1))
        palette.put("minecraft:oak_planks", IntTag(2))
        palette.put("minecraft:glass", IntTag(3))
        root.put("Palette", palette)

        val indices = IntArray(27) { i -> i % 4 }
        root.put("BlockData", ByteArrayTag(VarIntUtil.writeVarIntArray(indices)))

        schemFile.outputStream().use { fos ->
            NBTOutputStream(fos).writeTag(NamedTag("Schematic", root), 512)
        }

        // 2. 执行 SDK 转换
        val mcworldFile = File(tempDir, "output_superflat.mcworld")

        val result = McworldConverter.builder()
            .source(schemFile)
            .worldName("Superflat Test World")
            .terrain(WorldTerrainType.SUPERFLAT)
            .alignment(AlignmentMode.GROUND_ALIGNED)
            .targetVersion(BedrockVersion.V1_21)
            .convert(mcworldFile)

        // 3. 验证转换结果统计
        assertTrue(result.success)
        assertEquals("Superflat Test World", result.worldName)
        assertEquals(27L, result.totalBlocksConverted)
        assertTrue(result.chunksGenerated > 0)
        assertTrue(result.subChunksWritten > 0)
        assertTrue(mcworldFile.exists())
        assertTrue(mcworldFile.length() > 0)

        // 4. 解压并验证 .mcworld 内部结构
        val extractedDir = File(tempDir, "extracted_world")
        extractedDir.mkdirs()
        unzip(mcworldFile, extractedDir)

        val levelDat = File(extractedDir, "level.dat")
        val levelName = File(extractedDir, "levelname.txt")
        val worldIcon = File(extractedDir, "world_icon.jpeg")
        val dbDir = File(extractedDir, "db")

        assertTrue(levelDat.exists(), "level.dat should exist")
        assertTrue(levelName.exists(), "levelname.txt should exist")
        assertEquals("Superflat Test World", levelName.readText(Charsets.UTF_8))
        assertTrue(worldIcon.exists(), "world_icon.jpeg should exist")
        assertTrue(dbDir.exists() && dbDir.isDirectory, "db directory should exist")

        // 5. 解析并验证 level.dat
        val (storageVersion, levelDatCompound) = levelDat.inputStream().use {
            LittleEndianNbtUtil.readLevelDat(it)
        }
        assertEquals(10, storageVersion)
        assertEquals("Superflat Test World", levelDatCompound.getString("LevelName"))
        assertEquals(1, levelDatCompound.getInt("GameType")) // Creative

        // 6. 使用 LevelDB 校验写入的 Chunk 键值记录
        val db = Iq80DBFactory.factory.open(dbDir, Options())
        try {
            var foundSubChunk = false
            var foundChunkVersion = false

            db.iterator().use { iterator ->
                iterator.seekToFirst()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val key = entry.key
                    if (key.size == 9 || key.size == 10) {
                        val tag = key[8]
                        if (tag == 0x2F.toByte()) { // SubChunk Prefix
                            foundSubChunk = true
                            assertTrue(entry.value.isNotEmpty())
                            assertEquals(8.toByte(), entry.value[0]) // SubChunk v8
                        } else if (tag == 0x76.toByte()) { // Version tag
                            foundChunkVersion = true
                            assertEquals(40.toByte(), entry.value[0])
                        }
                    }
                }
            }

            assertTrue(foundSubChunk, "SubChunk record (0x2F) should be written to LevelDB")
            assertTrue(foundChunkVersion, "ChunkVersion record (0x76) should be written to LevelDB")
        } finally {
            db.close()
        }
    }

    @Test
    fun `test stream to stream convert for Android compatibility`() {
        // 创建内存中的测试 .schem 数据流
        val root = CompoundTag()
        root.put("Width", ShortTag(2))
        root.put("Height", ShortTag(2))
        root.put("Length", ShortTag(2))

        val palette = CompoundTag()
        palette.put("minecraft:stone", IntTag(0))
        palette.put("minecraft:dirt", IntTag(1))
        root.put("Palette", palette)

        val indices = intArrayOf(0, 1, 1, 0, 0, 1, 0, 1)
        root.put("BlockData", ByteArrayTag(VarIntUtil.writeVarIntArray(indices)))

        val inBaos = ByteArrayOutputStream()
        NBTOutputStream(inBaos).writeTag(NamedTag("Schematic", root), 512)

        val inputStream = ByteArrayInputStream(inBaos.toByteArray())
        val outputStream = ByteArrayOutputStream()

        // 执行流转换
        val result = McworldConverter.builder()
            .source(inputStream)
            .worldName("Stream World")
            .terrain(WorldTerrainType.VOID)
            .targetPosition(0, 64, 0)
            .convert(outputStream)

        assertTrue(result.success)
        assertEquals("Stream World", result.worldName)
        assertTrue(outputStream.size() > 0)
    }

    @Test
    fun `test custom terrain layers conversion`(@TempDir tempDir: File) {
        val schemFile = File(tempDir, "test.schem")
        val root = CompoundTag()
        root.put("Width", ShortTag(1))
        root.put("Height", ShortTag(1))
        root.put("Length", ShortTag(1))
        val palette = CompoundTag()
        palette.put("minecraft:diamond_block", IntTag(0))
        root.put("Palette", palette)
        root.put("BlockData", ByteArrayTag(VarIntUtil.writeVarIntArray(intArrayOf(0))))

        schemFile.outputStream().use {
            NBTOutputStream(it).writeTag(NamedTag("Schematic", root), 512)
        }

        val mcworldFile = File(tempDir, "custom_terrain.mcworld")
        val result = McworldConverter.builder()
            .source(schemFile)
            .worldName("Custom Layer World")
            .customLayers(
                WorldLayer(BedrockBlockState.BEDROCK, 1),
                WorldLayer(BedrockBlockState.STONE, 5),
                WorldLayer(BedrockBlockState.SAND, 3)
            )
            .alignment(AlignmentMode.GROUND_ALIGNED)
            .convert(mcworldFile)

        assertTrue(result.success)
        assertTrue(mcworldFile.exists())
    }

    @Test
    fun `test end-to-end convert legacy schematic to mcworld with void terrain and custom Y`(@TempDir tempDir: File) {
        val schematicFile = File(tempDir, "legacy_house.schematic")
        val root = CompoundTag()
        root.put("Width", ShortTag(2))
        root.put("Height", ShortTag(2))
        root.put("Length", ShortTag(2))
        root.put("Materials", StringTag("Alpha"))
        root.put("Blocks", ByteArrayTag(byteArrayOf(1, 4, 5, 20, 35, 41, 57, 89)))
        root.put("Data", ByteArrayTag(byteArrayOf(0, 0, 1, 0, 14, 0, 0, 0)))

        schematicFile.outputStream().use { fos ->
            NBTOutputStream(fos).writeTag(NamedTag("Schematic", root), 512)
        }

        val mcworldFile = File(tempDir, "legacy_void.mcworld")
        val result = McworldConverter.builder()
            .source(schematicFile)
            .worldName("Legacy Void World")
            .terrain(WorldTerrainType.VOID)
            .targetPosition(10, 100, -20)
            .alignment(AlignmentMode.ABSOLUTE)
            .convert(mcworldFile)

        assertTrue(result.success)
        assertEquals("Legacy Void World", result.worldName)
        assertEquals(8L, result.totalBlocksConverted)
        assertTrue(mcworldFile.exists())
        assertTrue(mcworldFile.length() > 0)
    }

    private fun unzip(zipFile: File, destDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
