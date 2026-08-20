package com.github.schem2mcworld.api

import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.model.SchematicData
import com.github.schem2mcworld.core.model.UniversalBlock
import com.github.schem2mcworld.core.model.Vector3i
import com.github.schem2mcworld.core.writer.LevelDbHelper
import com.github.schem2mcworld.core.writer.SubChunkEncoder
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipFile

class ExistingWorldInjectionTest {

    @Test
    fun `test multi-schematic placement into existing base world`(@TempDir tempDir: File) {
        // 1. 创建基底投影：石头塔位于原点 (0, 64, 0)
        val baseBlocks = Array(2 * 4 * 2) { UniversalBlock(id = "stone") }
        val baseSchematic = SchematicData(width = 2, height = 4, length = 2, blocks = baseBlocks)

        val baseWorldFile = File(tempDir, "base_world.mcworld")
        val baseResult = McworldConverter.builder()
            .source(baseSchematic.toTempFile(tempDir, "base.schem"))
            .worldName("Survival World")
            .terrain(WorldTerrainType.SUPERFLAT)
            .targetPosition(0, 64, 0)
            .convert(baseWorldFile)

        assertTrue(baseResult.success)
        assertTrue(baseWorldFile.exists())

        // 2. 创建第二个投影：钻石方块位于 (100, 64, 100)
        val diamondBlocks = Array(3 * 3 * 3) { UniversalBlock(id = "diamond_block") }
        val diamondSchematic = SchematicData(width = 3, height = 3, length = 3, blocks = diamondBlocks)
        val diamondFile = diamondSchematic.toTempFile(tempDir, "diamond.schem")

        // 3. 创建第三个投影：黄金方块位于 (-100, 70, -100)
        val goldBlocks = Array(4 * 2 * 4) { UniversalBlock(id = "gold_block") }
        val goldSchematic = SchematicData(width = 4, height = 2, length = 4, blocks = goldBlocks)
        val goldFile = goldSchematic.toTempFile(tempDir, "gold.schem")

        // 4. 将两个新投影同时注入到已有世界中
        val updatedWorldFile = File(tempDir, "updated_world.mcworld")
        val injectResult = McworldConverter.builder()
            .baseWorld(baseWorldFile)
            .addSchematic(diamondFile, x = 100, y = 64, z = 100)
            .addSchematic(goldFile, x = -100, y = 70, z = -100)
            .convert(updatedWorldFile)

        assertTrue(injectResult.success)
        assertTrue(updatedWorldFile.exists())
        assertTrue(updatedWorldFile.length() > 0)

        // 5. 解包验证 LevelDB 数据
        val unzippedDbDir = File(tempDir, "unzipped_db")
        unzipWorld(updatedWorldFile, unzippedDbDir)

        val dbDir = File(unzippedDbDir, "db")
        assertTrue(dbDir.exists())

        val db = Iq80DBFactory.factory.open(dbDir, Options().apply { createIfMissing(false) })
        try {
            // A. 验证原有原点 (0, 64, 0) 处的石块依然完好
            // chunkX = 0 shr 4 = 0, chunkZ = 0, subChunkY = 64 shr 4 = 4
            val subKey0 = LevelDbHelper.createSubChunkKey(0, 0, 4)
            val sub0Bytes = db.get(subKey0)
            assertNotNull(sub0Bytes, "Original subchunk (0,0,4) should exist")
            val sub0Blocks = SubChunkEncoder.decode(sub0Bytes)
            assertNotNull(sub0Blocks)
            // (0, 64, 0) -> lx=0, ly=0, lz=0 in subchunk 4
            val idx0 = SubChunkEncoder.getLocalIndex(0, 0, 0)
            assertEquals("minecraft:stone", sub0Blocks!![idx0].name)

            // B. 验证 (100, 64, 100) 处的钻石方块
            // 100 shr 4 = 6, 100 shr 4 = 6, 64 shr 4 = 4
            val subKey1 = LevelDbHelper.createSubChunkKey(6, 6, 4)
            val sub1Bytes = db.get(subKey1)
            assertNotNull(sub1Bytes, "Injected diamond subchunk (6,6,4) should exist")
            val sub1Blocks = SubChunkEncoder.decode(sub1Bytes)
            assertNotNull(sub1Blocks)
            // lx = 100 % 16 = 4, ly = 0, lz = 100 % 16 = 4
            val idx1 = SubChunkEncoder.getLocalIndex(4, 0, 4)
            assertEquals("minecraft:diamond_block", sub1Blocks!![idx1].name)

            // C. 验证 (-100, 70, -100) 处的黄金方块
            // -100 shr 4 = -7, -100 shr 4 = -7, 70 shr 4 = 4
            val subKey2 = LevelDbHelper.createSubChunkKey(-7, -7, 4)
            val sub2Bytes = db.get(subKey2)
            assertNotNull(sub2Bytes, "Injected gold subchunk (-7,-7,4) should exist")
            val sub2Blocks = SubChunkEncoder.decode(sub2Bytes)
            assertNotNull(sub2Blocks)
            // lx = ((-100 % 16) + 16) % 16 = 12, ly = 70 % 16 = 6, lz = 12
            val idx2 = SubChunkEncoder.getLocalIndex(12, 6, 12)
            assertEquals("minecraft:gold_block", sub2Blocks!![idx2].name)

        } finally {
            db.close()
        }
    }

    private fun SchematicData.toTempFile(dir: File, name: String): File {
        val root = net.querz.nbt.tag.CompoundTag()
        root.putInt("Version", 2)
        root.putInt("DataVersion", 2586)
        root.putShort("Width", width.toShort())
        root.putShort("Height", height.toShort())
        root.putShort("Length", length.toShort())

        val palette = net.querz.nbt.tag.CompoundTag()
        var nextId = 0
        val paletteMap = HashMap<String, Int>()
        val blockData = ByteArray(totalBlocks)

        for (i in blocks.indices) {
            val stateStr = blocks[i].toStateString()
            val id = paletteMap.computeIfAbsent(stateStr) {
                palette.putInt(stateStr, nextId)
                nextId++
            }
            blockData[i] = id.toByte()
        }

        root.put("Palette", palette)
        root.put("BlockData", net.querz.nbt.tag.ByteArrayTag(blockData))

        val f = File(dir, name)
        f.outputStream().use { fos ->
            java.util.zip.GZIPOutputStream(fos).use { gz ->
                net.querz.nbt.io.NBTOutputStream(gz).writeTag(net.querz.nbt.io.NamedTag("Schematic", root), 512)
            }
        }
        return f
    }

    private fun unzipWorld(zipFile: File, destDir: File) {
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
}
