package com.github.schem2mcworld.core.parser

import com.github.schem2mcworld.core.model.BlockEntity
import com.github.schem2mcworld.core.model.SchematicData
import com.github.schem2mcworld.core.model.UniversalBlock
import com.github.schem2mcworld.core.model.Vector3i
import com.github.schem2mcworld.core.util.NbtHelper
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.ListTag
import net.querz.nbt.tag.LongArrayTag
import java.io.InputStream
import kotlin.math.abs

class LitematicaSchematicParser : SchematicParser {

    override fun supports(format: SchematicFormat): Boolean =
        format == SchematicFormat.LITEMATICA_SCHEMATIC

    override fun parse(inputStream: InputStream): SchematicData {
        val root = NbtHelper.readCompoundTag(inputStream)
        val regionsTag = root.getCompoundTag("Regions")
            ?: throw IllegalArgumentException("Invalid .litematic format: Missing 'Regions' compound tag")

        if (regionsTag.size() == 0) {
            throw IllegalArgumentException("Invalid .litematic format: 'Regions' contains no sub-regions")
        }

        val parsedRegions = mutableListOf<ParsedRegion>()

        for ((regionName, regionRaw) in regionsTag) {
            val region = regionRaw as? CompoundTag ?: continue
            val posTag = region.getCompoundTag("Position") ?: CompoundTag()
            val sizeTag = region.getCompoundTag("Size") ?: CompoundTag()

            val posX = posTag.getInt("x")
            val posY = posTag.getInt("y")
            val posZ = posTag.getInt("z")

            val rawSizeX = sizeTag.getInt("x")
            val rawSizeY = sizeTag.getInt("y")
            val rawSizeZ = sizeTag.getInt("z")

            val absSizeX = abs(rawSizeX)
            val absSizeY = abs(rawSizeY)
            val absSizeZ = abs(rawSizeZ)

            if (absSizeX == 0 || absSizeY == 0 || absSizeZ == 0) continue

            val minX = if (rawSizeX < 0) posX + rawSizeX + 1 else posX
            val minY = if (rawSizeY < 0) posY + rawSizeY + 1 else posY
            val minZ = if (rawSizeZ < 0) posZ + rawSizeZ + 1 else posZ

            val maxX = minX + absSizeX - 1
            val maxY = minY + absSizeY - 1
            val maxZ = minZ + absSizeZ - 1

            // 1. 解析调色板
            val palette = mutableListOf<UniversalBlock>()
            val paletteTag = region.getListTag("BlockStatePalette")
            if (paletteTag != null) {
                for (tag in paletteTag) {
                    val comp = tag as? CompoundTag ?: continue
                    val name = comp.getString("Name")
                    val propsComp = comp.getCompoundTag("Properties")
                    val props = mutableMapOf<String, String>()
                    if (propsComp != null) {
                        for ((k, v) in propsComp) {
                            props[k] = v.valueToString().replace("\"", "")
                        }
                    }
                    val parts = name.split(":", limit = 2)
                    val ns = if (parts.size == 2) parts[0] else "minecraft"
                    val id = if (parts.size == 2) parts[1] else parts[0]
                    palette.add(UniversalBlock(namespace = ns, id = id, properties = props))
                }
            }

            if (palette.isEmpty()) {
                palette.add(UniversalBlock.AIR)
            }

            // 2. 解包 LongArray BlockStates
            val blockStatesTag = region.get("BlockStates") as? LongArrayTag
            val blockStatesLongs = blockStatesTag?.value
            val totalBlocks = absSizeX * absSizeY * absSizeZ
            val regionBlocks = Array(totalBlocks) { UniversalBlock.AIR }

            if (blockStatesLongs != null && blockStatesLongs.isNotEmpty()) {
                var bits = 2
                while ((1 shl bits) < palette.size) {
                    bits++
                }
                val maxMask = (1L shl bits) - 1L

                for (i in 0 until totalBlocks) {
                    val startBit = i.toLong() * bits
                    val startLong = (startBit / 64).toInt()
                    val endLong = ((startBit + bits - 1) / 64).toInt()
                    val startBitInLong = (startBit % 64).toInt()

                    val paletteIndex = if (startLong == endLong) {
                        if (startLong < blockStatesLongs.size) {
                            ((blockStatesLongs[startLong] ushr startBitInLong) and maxMask).toInt()
                        } else 0
                    } else {
                        val endBitInLong = 64 - startBitInLong
                        val part1 = if (startLong < blockStatesLongs.size) {
                            blockStatesLongs[startLong] ushr startBitInLong
                        } else 0L
                        val part2 = if (endLong < blockStatesLongs.size) {
                            blockStatesLongs[endLong] shl endBitInLong
                        } else 0L
                        ((part1 or part2) and maxMask).toInt()
                    }

                    if (paletteIndex in palette.indices) {
                        regionBlocks[i] = palette[paletteIndex]
                    } else {
                        regionBlocks[i] = palette[0]
                    }
                }
            } else {
                val defaultBlock = palette[0]
                for (i in 0 until totalBlocks) {
                    regionBlocks[i] = defaultBlock
                }
            }

            // 3. 解析方块实体
            val blockEntities = mutableListOf<BlockEntity>()
            val beList = (region.getListTag("BlockEntities") ?: region.getListTag("TileEntities"))
            if (beList != null) {
                for (beTag in beList) {
                    val beComp = beTag as? CompoundTag ?: continue
                    val xVal = beComp.getInt("x")
                    val yVal = beComp.getInt("y")
                    val zVal = beComp.getInt("z")
                    val id = beComp.getString("id").ifEmpty { beComp.getString("Id") }

                    val beX = if (xVal in 0 until absSizeX) minX + xVal else xVal
                    val beY = if (yVal in 0 until absSizeY) minY + yVal else yVal
                    val beZ = if (zVal in 0 until absSizeZ) minZ + zVal else zVal

                    blockEntities.add(BlockEntity(Vector3i(beX, beY, beZ), id, beComp))
                }
            }

            parsedRegions.add(
                ParsedRegion(
                    name = regionName,
                    minX = minX,
                    minY = minY,
                    minZ = minZ,
                    maxX = maxX,
                    maxY = maxY,
                    maxZ = maxZ,
                    sizeX = absSizeX,
                    sizeY = absSizeY,
                    sizeZ = absSizeZ,
                    blocks = regionBlocks,
                    blockEntities = blockEntities
                )
            )
        }

        if (parsedRegions.isEmpty()) {
            throw IllegalArgumentException("Invalid .litematic: No non-empty regions found")
        }

        // 4. 合并所有区域到全局外接包围盒
        val globalMinX = parsedRegions.minOf { it.minX }
        val globalMinY = parsedRegions.minOf { it.minY }
        val globalMinZ = parsedRegions.minOf { it.minZ }

        val globalMaxX = parsedRegions.maxOf { it.maxX }
        val globalMaxY = parsedRegions.maxOf { it.maxY }
        val globalMaxZ = parsedRegions.maxOf { it.maxZ }

        val totalWidth = globalMaxX - globalMinX + 1
        val totalHeight = globalMaxY - globalMinY + 1
        val totalLength = globalMaxZ - globalMinZ + 1

        val finalBlocks = Array(totalWidth * totalHeight * totalLength) { UniversalBlock.AIR }
        val finalBlockEntities = mutableListOf<BlockEntity>()

        for (region in parsedRegions) {
            for (ry in 0 until region.sizeY) {
                for (rz in 0 until region.sizeZ) {
                    for (rx in 0 until region.sizeX) {
                        val localIndex = (ry * region.sizeZ + rz) * region.sizeX + rx
                        val block = region.blocks[localIndex]
                        if (block != UniversalBlock.AIR) {
                            val globalX = (region.minX + rx) - globalMinX
                            val globalY = (region.minY + ry) - globalMinY
                            val globalZ = (region.minZ + rz) - globalMinZ

                            val targetIndex = (globalY * totalLength + globalZ) * totalWidth + globalX
                            if (targetIndex in finalBlocks.indices) {
                                finalBlocks[targetIndex] = block
                            }
                        }
                    }
                }
            }

            for (be in region.blockEntities) {
                val relPos = Vector3i(
                    be.position.x - globalMinX,
                    be.position.y - globalMinY,
                    be.position.z - globalMinZ
                )
                finalBlockEntities.add(BlockEntity(relPos, be.id, be.data))
            }
        }

        return SchematicData(
            width = totalWidth,
            height = totalHeight,
            length = totalLength,
            blocks = finalBlocks,
            blockEntities = finalBlockEntities,
            offset = Vector3i(globalMinX, globalMinY, globalMinZ)
        )
    }

    private data class ParsedRegion(
        val name: String,
        val minX: Int,
        val minY: Int,
        val minZ: Int,
        val maxX: Int,
        val maxY: Int,
        val maxZ: Int,
        val sizeX: Int,
        val sizeY: Int,
        val sizeZ: Int,
        val blocks: Array<UniversalBlock>,
        val blockEntities: List<BlockEntity>
    )
}
