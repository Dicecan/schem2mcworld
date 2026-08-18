package com.github.schem2mcworld.parser

import com.github.schem2mcworld.core.parser.LegacySchematicParser
import com.github.schem2mcworld.core.parser.ParserFactory
import com.github.schem2mcworld.core.parser.SchematicFormat
import net.querz.nbt.io.NBTOutputStream
import net.querz.nbt.io.NamedTag
import net.querz.nbt.tag.ByteArrayTag
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.ShortTag
import net.querz.nbt.tag.StringTag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class LegacySchematicParserTest {

    private val parser = LegacySchematicParser()

    @Test
    fun `test parse valid legacy schematic`() {
        val root = CompoundTag()
        root.put("Width", ShortTag(2))
        root.put("Height", ShortTag(2))
        root.put("Length", ShortTag(2))
        root.put("Materials", StringTag("Alpha"))

        // Blocks: 8 blocks
        // ID 1 (Stone), ID 1 (Stone data 1 -> Granite), ID 2 (Grass), ID 3 (Dirt)
        // ID 5 (Planks data 0 -> Oak), ID 5 (Planks data 1 -> Spruce), ID 35 (Wool data 0 -> White), ID 35 (Wool data 1 -> Orange)
        val blocks = byteArrayOf(1, 1, 2, 3, 5, 5, 35, 35)
        val data = byteArrayOf(0, 1, 0, 0, 0, 1, 0, 1)

        root.put("Blocks", ByteArrayTag(blocks))
        root.put("Data", ByteArrayTag(data))

        val baos = ByteArrayOutputStream()
        NBTOutputStream(baos).writeTag(NamedTag("Schematic", root), 512)
        val nbtBytes = baos.toByteArray()

        // 格式探测
        val detected = ParserFactory.detectFormat(nbtBytes)
        assertEquals(SchematicFormat.LEGACY_SCHEMATIC, detected)

        // 解析验证
        val schematic = parser.parse(ByteArrayInputStream(nbtBytes))
        assertEquals(2, schematic.width)
        assertEquals(2, schematic.height)
        assertEquals(2, schematic.length)
        assertEquals(8, schematic.totalBlocks)

        // 检查解析出的 UniversalBlock
        val b0 = schematic.getBlock(0, 0, 0)
        assertEquals("minecraft:stone", b0.fullId)

        val b1 = schematic.getBlock(1, 0, 0)
        assertEquals("minecraft:granite", b1.fullId)

        val b5 = schematic.getBlock(1, 1, 0)
        assertEquals("minecraft:spruce_planks", b5.fullId)

        val b7 = schematic.getBlock(1, 1, 1)
        assertEquals("minecraft:orange_wool", b7.fullId)
    }
}
