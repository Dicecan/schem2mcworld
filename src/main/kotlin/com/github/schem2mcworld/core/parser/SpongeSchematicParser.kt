package com.github.schem2mcworld.core.parser

import net.querz.nbt.tag.ByteArrayTag
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.IntTag
import net.querz.nbt.tag.NumberTag
import com.github.schem2mcworld.core.model.BlockEntity
import com.github.schem2mcworld.core.model.SchematicData
import com.github.schem2mcworld.core.model.UniversalBlock
import com.github.schem2mcworld.core.model.Vector3i
import com.github.schem2mcworld.core.util.NbtHelper
import com.github.schem2mcworld.core.util.VarIntUtil
import java.io.InputStream

class SpongeSchematicParser : SchematicParser {

    override fun supports(format: SchematicFormat): Boolean =
        format == SchematicFormat.SPONGE_SCHEMATIC

    override fun parse(inputStream: InputStream): SchematicData {
        var root = NbtHelper.readCompoundTag(inputStream)

        val schematicCompound = root.get("Schematic") as? CompoundTag
        if (schematicCompound != null) {
            root = schematicCompound
        }

        val width = if (root.containsKey("Width")) (root.get("Width") as? NumberTag<*>)?.asInt() ?: root.getShort("Width").toInt()
        else throw IllegalArgumentException("Missing 'Width' tag in .schem")

        val height = if (root.containsKey("Height")) (root.get("Height") as? NumberTag<*>)?.asInt() ?: root.getShort("Height").toInt()
        else throw IllegalArgumentException("Missing 'Height' tag in .schem")

        val length = if (root.containsKey("Length")) (root.get("Length") as? NumberTag<*>)?.asInt() ?: root.getShort("Length").toInt()
        else throw IllegalArgumentException("Missing 'Length' tag in .schem")

        val totalBlocks = width * height * length

        val paletteMap = mutableMapOf<Int, UniversalBlock>()
        val blocksCompound = root.get("Blocks") as? CompoundTag
        val paletteTag = (blocksCompound?.getCompoundTag("Palette")) ?: root.getCompoundTag("Palette")
            ?: throw IllegalArgumentException("Missing 'Palette' tag in .schem")

        for (entry in paletteTag.entrySet()) {
            val stateString = entry.key
            val tag = entry.value
            val paletteIndex = when (tag) {
                is IntTag -> tag.asInt()
                is NumberTag<*> -> tag.asInt()
                else -> continue
            }
            paletteMap[paletteIndex] = UniversalBlock.fromStateString(stateString)
        }

        val blockDataBytes = (blocksCompound?.getByteArray("Data"))
            ?: root.getByteArray("BlockData")
            ?: (root.get("Blocks") as? ByteArrayTag)?.value
            ?: throw IllegalArgumentException("Missing 'BlockData' / 'Data' tag in .schem")

        val blockIndices = VarIntUtil.readVarIntArray(blockDataBytes, totalBlocks)
        val blocks = Array(totalBlocks) { i ->
            val paletteIndex = blockIndices[i]
            paletteMap[paletteIndex] ?: UniversalBlock.AIR
        }

        val blockEntities = mutableListOf<BlockEntity>()
        val blockEntitiesTag = (blocksCompound?.getListTag("BlockEntities"))
            ?: root.getListTag("BlockEntities")
            ?: root.getListTag("TileEntities")

        if (blockEntitiesTag != null) {
            for (item in blockEntitiesTag) {
                if (item is CompoundTag) {
                    val posTag = item.getIntArray("Pos")
                    val x = posTag?.getOrNull(0) ?: item.getInt("x")
                    val y = posTag?.getOrNull(1) ?: item.getInt("y")
                    val z = posTag?.getOrNull(2) ?: item.getInt("z")
                    val id = item.getString("Id").ifBlank { item.getString("id") }
                    val data = item.getCompoundTag("Data") ?: item
                    if (id.isNotBlank()) {
                        blockEntities.add(BlockEntity(Vector3i(x, y, z), id, data))
                    }
                }
            }
        }

        val offsetTag = root.getIntArray("Offset")
            ?: (root.get("Metadata") as? CompoundTag)?.getIntArray("WEOffset")
        val offset = if (offsetTag != null && offsetTag.size >= 3) {
            Vector3i(offsetTag[0], offsetTag[1], offsetTag[2])
        } else {
            Vector3i.ZERO
        }

        return SchematicData(
            width = width,
            height = height,
            length = length,
            blocks = blocks,
            blockEntities = blockEntities,
            offset = offset
        )
    }
}
