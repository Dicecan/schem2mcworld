package com.github.schem2mcworld.parser

import com.github.schem2mcworld.core.parser.ParserFactory
import com.github.schem2mcworld.core.parser.SchematicFormat
import com.github.schem2mcworld.core.parser.SpongeSchematicParser
import com.github.schem2mcworld.core.util.VarIntUtil
import net.querz.nbt.io.NBTOutputStream
import net.querz.nbt.io.NamedTag
import net.querz.nbt.tag.ByteArrayTag
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.IntTag
import net.querz.nbt.tag.ShortTag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SpongeSchematicParserTest {

    private val parser = SpongeSchematicParser()

    @Test
    fun `test parse valid sponge schematic`() {
        val root = CompoundTag()
        root.put("Width", ShortTag(2))
        root.put("Height", ShortTag(2))
        root.put("Length", ShortTag(2))
        root.put("Version", IntTag(2))
        root.put("DataVersion", IntTag(2586))

        // Palette
        val paletteTag = CompoundTag()
        paletteTag.put("minecraft:air", IntTag(0))
        paletteTag.put("minecraft:stone", IntTag(1))
        paletteTag.put("minecraft:oak_stairs[facing=north,half=bottom]", IntTag(2))
        root.put("Palette", paletteTag)

        // BlockData: 8 blocks
        val indices = intArrayOf(0, 1, 1, 2, 2, 1, 0, 1)
        val varIntBytes = VarIntUtil.writeVarIntArray(indices)
        root.put("BlockData", ByteArrayTag(varIntBytes))

        val baos = ByteArrayOutputStream()
        NBTOutputStream(baos).writeTag(NamedTag("Schematic", root), 512)
        val nbtBytes = baos.toByteArray()

        // 格式探测
        val detected = ParserFactory.detectFormat(nbtBytes)
        assertEquals(SchematicFormat.SPONGE_SCHEMATIC, detected)

        // 解析验证
        val schematic = parser.parse(ByteArrayInputStream(nbtBytes))
        assertEquals(2, schematic.width)
        assertEquals(2, schematic.height)
        assertEquals(2, schematic.length)
        assertEquals(8, schematic.totalBlocks)

        // 检查方块
        val b0 = schematic.getBlock(0, 0, 0)
        assertEquals("minecraft:air", b0.fullId)

        val b1 = schematic.getBlock(1, 0, 0)
        assertEquals("minecraft:stone", b1.fullId)

        val b3 = schematic.getBlock(1, 0, 1)
        assertEquals("minecraft:oak_stairs", b3.fullId)
        assertEquals("north", b3.properties["facing"])
        assertEquals("bottom", b3.properties["half"])
    }
}
