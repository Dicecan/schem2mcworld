package com.github.schem2mcworld.core.parser

import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.NumberTag
import com.github.schem2mcworld.core.mapper.MappingDataLoader
import com.github.schem2mcworld.core.model.BlockEntity
import com.github.schem2mcworld.core.model.SchematicData
import com.github.schem2mcworld.core.model.UniversalBlock
import com.github.schem2mcworld.core.model.Vector3i
import com.github.schem2mcworld.core.util.NbtHelper
import java.io.InputStream

class LegacySchematicParser(
    private val legacyMapping: Map<String, String> = MappingDataLoader.loadDefaultLegacyToJava()
) : SchematicParser {

    override fun supports(format: SchematicFormat): Boolean =
        format == SchematicFormat.LEGACY_SCHEMATIC

    override fun parse(inputStream: InputStream): SchematicData {
        val root = NbtHelper.readCompoundTag(inputStream)

        val width = if (root.containsKey("Width")) (root.get("Width") as? NumberTag<*>)?.asInt() ?: root.getShort("Width").toInt()
        else throw IllegalArgumentException("Missing 'Width' tag in .schematic")

        val height = if (root.containsKey("Height")) (root.get("Height") as? NumberTag<*>)?.asInt() ?: root.getShort("Height").toInt()
        else throw IllegalArgumentException("Missing 'Height' tag in .schematic")

        val length = if (root.containsKey("Length")) (root.get("Length") as? NumberTag<*>)?.asInt() ?: root.getShort("Length").toInt()
        else throw IllegalArgumentException("Missing 'Length' tag in .schematic")

        val blocksData = root.getByteArray("Blocks")
            ?: throw IllegalArgumentException("Missing 'Blocks' byte array tag in .schematic")
        val metaData = root.getByteArray("Data") ?: ByteArray(blocksData.size)
        val addBlocks = root.getByteArray("AddBlocks")

        val totalBlocks = width * height * length
        require(blocksData.size >= totalBlocks) {
            "Blocks data length (${blocksData.size}) is smaller than total dimensions ($width x $height x $length = $totalBlocks)"
        }

        val blocks = Array(totalBlocks) { UniversalBlock.AIR }

        for (y in 0 until height) {
            for (z in 0 until length) {
                for (x in 0 until width) {
                    val index = (y * length + z) * width + x
                    var blockId = blocksData[index].toInt() and 0xFF

                    if (addBlocks != null) {
                        val addIndex = index / 2
                        if (addIndex < addBlocks.size) {
                            val addByte = addBlocks[addIndex].toInt() and 0xFF
                            val addNibble = if (index % 2 == 0) {
                                addByte and 0x0F
                            } else {
                                (addByte ushr 4) and 0x0F
                            }
                            blockId = blockId or (addNibble shl 8)
                        }
                    }

                    val dataVal = if (index < metaData.size) (metaData[index].toInt() and 0x0F) else 0
                    blocks[index] = resolveLegacyBlock(blockId, dataVal)
                }
            }
        }

        val blockEntities = mutableListOf<BlockEntity>()
        val tileEntitiesTag = root.getListTag("TileEntities") ?: root.getListTag("BlockEntities")
        if (tileEntitiesTag != null) {
            for (tag in tileEntitiesTag) {
                if (tag is CompoundTag) {
                    val x = tag.getInt("x")
                    val y = tag.getInt("y")
                    val z = tag.getInt("z")
                    val id = tag.getString("id")
                    if (id.isNotBlank()) {
                        blockEntities.add(BlockEntity(Vector3i(x, y, z), id, tag))
                    }
                }
            }
        }

        val weOffsetX = root.getInt("WEOffsetX")
        val weOffsetY = root.getInt("WEOffsetY")
        val weOffsetZ = root.getInt("WEOffsetZ")
        val offset = Vector3i(weOffsetX, weOffsetY, weOffsetZ)

        return SchematicData(
            width = width,
            height = height,
            length = length,
            blocks = blocks,
            blockEntities = blockEntities,
            offset = offset
        )
    }

    private fun resolveLegacyBlock(blockId: Int, dataValue: Int): UniversalBlock {
        if (blockId == 0) return UniversalBlock.AIR

        legacyMapping["$blockId:$dataValue"]?.let {
            return UniversalBlock.fromStateString(it)
        }

        legacyMapping["$blockId"]?.let {
            return UniversalBlock.fromStateString(it)
        }

        return UniversalBlock(namespace = "minecraft", id = "legacy_block_$blockId")
    }
}
