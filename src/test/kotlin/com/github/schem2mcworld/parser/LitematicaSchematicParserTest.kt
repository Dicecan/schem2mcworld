package com.github.schem2mcworld.parser

import com.github.schem2mcworld.api.AlignmentMode
import com.github.schem2mcworld.api.McworldConverter
import com.github.schem2mcworld.api.WorldTerrainType
import com.github.schem2mcworld.core.model.UniversalBlock
import com.github.schem2mcworld.core.parser.LitematicaSchematicParser
import com.github.schem2mcworld.core.parser.ParserFactory
import com.github.schem2mcworld.core.parser.SchematicFormat
import net.querz.nbt.io.NBTOutputStream
import net.querz.nbt.io.NamedTag
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.IntTag
import net.querz.nbt.tag.ListTag
import net.querz.nbt.tag.LongArrayTag
import net.querz.nbt.tag.StringTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPOutputStream

class LitematicaSchematicParserTest {

    @Test
    fun `test parse simple single-region litematic`() {
        val root = CompoundTag()
        root.putInt("Version", 5)
        root.putInt("MinecraftDataVersion", 2586)

        val metadata = CompoundTag()
        metadata.putString("Author", "Dicecan")
        metadata.putString("Name", "TestLitematic")
        metadata.putInt("TotalBlocks", 8)
        root.put("Metadata", metadata)

        val regions = CompoundTag()
        val reg = CompoundTag()

        val pos = CompoundTag()
        pos.putInt("x", 0)
        pos.putInt("y", 0)
        pos.putInt("z", 0)
        reg.put("Position", pos)

        val size = CompoundTag()
        size.putInt("x", 2)
        size.putInt("y", 2)
        size.putInt("z", 2)
        reg.put("Size", size)

        // Palette: air, stone, oak_planks, glass, diamond_block (5 blocks => 3 bits/block)
        val palette = ListTag(CompoundTag::class.java)
        palette.add(createBlockTag("minecraft:air"))
        palette.add(createBlockTag("minecraft:stone"))
        palette.add(createBlockTag("minecraft:oak_planks"))
        palette.add(createBlockTag("minecraft:glass"))
        palette.add(createBlockTag("minecraft:diamond_block"))
        reg.put("BlockStatePalette", palette)

        // Total blocks = 2 * 2 * 2 = 8
        // Bits = 3. 8 * 3 = 24 bits. Fits in 1 long.
        // Let's set indices: [0, 1, 2, 3, 4, 1, 2, 3]
        var longVal = 0L
        val indices = intArrayOf(0, 1, 2, 3, 4, 1, 2, 3)
        for (i in indices.indices) {
            longVal = longVal or (indices[i].toLong() shl (i * 3))
        }
        reg.put("BlockStates", LongArrayTag(longArrayOf(longVal)))

        regions.put("MainRegion", reg)
        root.put("Regions", regions)

        val gzipped = compressNbt(root)
        val format = ParserFactory.detectFormat(gzipped)
        assertEquals(SchematicFormat.LITEMATICA_SCHEMATIC, format)

        val parser = LitematicaSchematicParser()
        val schematic = parser.parse(ByteArrayInputStream(gzipped))

        assertEquals(2, schematic.width)
        assertEquals(2, schematic.height)
        assertEquals(2, schematic.length)
        assertEquals(8, schematic.totalBlocks)

        // Index 0: (0, 0, 0) -> air
        assertEquals("air", schematic.getBlock(0, 0, 0).id)
        // Index 1: (1, 0, 0) -> stone
        assertEquals("stone", schematic.getBlock(1, 0, 0).id)
        // Index 2: (0, 0, 1) -> oak_planks
        assertEquals("oak_planks", schematic.getBlock(0, 0, 1).id)
        // Index 3: (1, 0, 1) -> glass
        assertEquals("glass", schematic.getBlock(1, 0, 1).id)
        // Index 4: (0, 1, 0) -> diamond_block
        assertEquals("diamond_block", schematic.getBlock(0, 1, 0).id)
    }

    @Test
    fun `test parse multi-region litematic with negative size and block entity`(@TempDir tempDir: File) {
        val root = CompoundTag()
        root.putInt("Version", 5)

        val regions = CompoundTag()

        // Region 1: pos=(0, 0, 0), size=(-3, 2, -2) => extends in negative X and Z
        val reg1 = CompoundTag()
        val pos1 = CompoundTag()
        pos1.putInt("x", 0)
        pos1.putInt("y", 0)
        pos1.putInt("z", 0)
        reg1.put("Position", pos1)

        val size1 = CompoundTag()
        size1.putInt("x", -3)
        size1.putInt("y", 2)
        size1.putInt("z", -2)
        reg1.put("Size", size1)

        val pal1 = ListTag(CompoundTag::class.java)
        pal1.add(createBlockTag("minecraft:stone"))
        pal1.add(createBlockTag("minecraft:gold_block"))
        reg1.put("BlockStatePalette", pal1)

        // 3 * 2 * 2 = 12 blocks, 2 bits each = 24 bits
        val longs1 = LongArrayTag(longArrayOf(0x555555L)) // all index 1 (gold_block)
        reg1.put("BlockStates", longs1)

        // Block entity
        val beList = ListTag(CompoundTag::class.java)
        val chestBe = CompoundTag()
        chestBe.putString("id", "minecraft:chest")
        chestBe.putInt("x", 0)
        chestBe.putInt("y", 0)
        chestBe.putInt("z", 0)
        chestBe.putString("CustomName", "Treasure Chest")
        beList.add(chestBe)
        reg1.put("BlockEntities", beList)

        regions.put("Sub1", reg1)
        root.put("Regions", regions)

        val gzipped = compressNbt(root)
        val litematicFile = File(tempDir, "multi_test.litematic")
        litematicFile.writeBytes(gzipped)

        val schematic = ParserFactory.parse(litematicFile)
        assertEquals(3, schematic.width)
        assertEquals(2, schematic.height)
        assertEquals(2, schematic.length)
        assertEquals(1, schematic.blockEntities.size)

        // End-to-end convert to mcworld
        val mcworldFile = File(tempDir, "litematic_world.mcworld")
        val result = McworldConverter.builder()
            .source(litematicFile)
            .terrain(WorldTerrainType.SUPERFLAT)
            .alignment(AlignmentMode.GROUND_ALIGNED)
            .convert(mcworldFile)

        assertTrue(result.success)
        assertTrue(mcworldFile.exists())
        assertTrue(mcworldFile.length() > 0)
    }

    private fun createBlockTag(name: String, properties: Map<String, String> = emptyMap()): CompoundTag {
        val tag = CompoundTag()
        tag.putString("Name", name)
        if (properties.isNotEmpty()) {
            val props = CompoundTag()
            for ((k, v) in properties) {
                props.putString(k, v)
            }
            tag.put("Properties", props)
        }
        return tag
    }

    private fun compressNbt(tag: CompoundTag): ByteArray {
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { gz ->
            val nbtOut = NBTOutputStream(gz)
            nbtOut.writeTag(NamedTag("", tag), 512)
        }
        return baos.toByteArray()
    }
}
