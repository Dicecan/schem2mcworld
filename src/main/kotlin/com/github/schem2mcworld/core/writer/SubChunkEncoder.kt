package com.github.schem2mcworld.core.writer

import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.util.BitArray
import com.github.schem2mcworld.core.util.LittleEndianNbtUtil
import net.querz.nbt.tag.ByteTag
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.IntTag
import net.querz.nbt.tag.StringTag
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object SubChunkEncoder {

    const val SUBCHUNK_SIZE = 4096
    const val DEFAULT_STORAGE_VERSION: Byte = 8

    fun getLocalIndex(localX: Int, localY: Int, localZ: Int): Int =
        (localX shl 8) or (localZ shl 4) or localY

    fun encode(
        blocks: Array<BedrockBlockState>,
        storageVersion: Byte = DEFAULT_STORAGE_VERSION
    ): ByteArray {
        require(blocks.size == SUBCHUNK_SIZE) {
            "SubChunk blocks array must have exactly $SUBCHUNK_SIZE elements, got ${blocks.size}"
        }

        val palette = mutableListOf<BedrockBlockState>()
        val paletteMap = HashMap<BedrockBlockState, Int>()

        for (b in blocks) {
            if (!paletteMap.containsKey(b)) {
                paletteMap[b] = palette.size
                palette.add(b)
            }
        }

        val bitsPerBlock = BitArray.getOptimalBitsPerBlock(palette.size)
        val bitArray = BitArray(bitsPerBlock, SUBCHUNK_SIZE)

        for (i in 0 until SUBCHUNK_SIZE) {
            val paletteIndex = paletteMap[blocks[i]] ?: 0
            bitArray[i] = paletteIndex
        }

        val baos = ByteArrayOutputStream(SUBCHUNK_SIZE + palette.size * 64)

        baos.write(storageVersion.toInt())
        baos.write(1)

        val layerHeader = (bitsPerBlock shl 1) or 0
        baos.write(layerHeader)

        for (word in bitArray.words) {
            baos.write(word and 0xFF)
            baos.write((word ushr 8) and 0xFF)
            baos.write((word ushr 16) and 0xFF)
            baos.write((word ushr 24) and 0xFF)
        }

        val paletteSize = palette.size
        baos.write(paletteSize and 0xFF)
        baos.write((paletteSize ushr 8) and 0xFF)
        baos.write((paletteSize ushr 16) and 0xFF)
        baos.write((paletteSize ushr 24) and 0xFF)

        for (state in palette) {
            LittleEndianNbtUtil.writeTagLE(state.toNbt(), baos)
        }

        return baos.toByteArray()
    }

    fun decode(bytes: ByteArray): Array<BedrockBlockState>? {
        if (bytes.size < 3) return null
        return try {
            var offset = 0
            val storageVersion = bytes[offset++].toInt() and 0xFF
            val layerCount = bytes[offset++].toInt() and 0xFF

            if (layerCount <= 0) return null

            val layerHeader = bytes[offset++].toInt() and 0xFF
            val bitsPerBlock = layerHeader ushr 1

            if (bitsPerBlock == 0) {
                if (offset + 4 > bytes.size) return null
                val paletteSize = (bytes[offset].toInt() and 0xFF) or
                        ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                        ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                        ((bytes[offset + 3].toInt() and 0xFF) shl 24)
                offset += 4

                if (paletteSize <= 0 || offset >= bytes.size) {
                    return Array(SUBCHUNK_SIZE) { BedrockBlockState.AIR }
                }

                val bais = ByteArrayInputStream(bytes, offset, bytes.size - offset)
                val rootTag = LittleEndianNbtUtil.readTagLE(bais) as? CompoundTag
                    ?: return Array(SUBCHUNK_SIZE) { BedrockBlockState.AIR }

                val singleState = nbtToBedrockState(rootTag)
                return Array(SUBCHUNK_SIZE) { singleState }
            }

            val wordsCount = BitArray.calculateWordCount(bitsPerBlock, SUBCHUNK_SIZE)
            if (offset + wordsCount * 4 + 4 > bytes.size) return null

            val words = IntArray(wordsCount)
            for (w in 0 until wordsCount) {
                val wVal = (bytes[offset].toInt() and 0xFF) or
                        ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                        ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                        ((bytes[offset + 3].toInt() and 0xFF) shl 24)
                words[w] = wVal
                offset += 4
            }

            val paletteSize = (bytes[offset].toInt() and 0xFF) or
                    ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                    ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                    ((bytes[offset + 3].toInt() and 0xFF) shl 24)
            offset += 4

            val palette = mutableListOf<BedrockBlockState>()
            val bais = ByteArrayInputStream(bytes, offset, bytes.size - offset)

            for (p in 0 until paletteSize) {
                val tag = LittleEndianNbtUtil.readTagLE(bais) as? CompoundTag ?: break
                palette.add(nbtToBedrockState(tag))
            }

            if (palette.isEmpty()) {
                palette.add(BedrockBlockState.AIR)
            }

            val bitArray = BitArray(bitsPerBlock, SUBCHUNK_SIZE, words)
            val result = Array(SUBCHUNK_SIZE) { BedrockBlockState.AIR }

            for (i in 0 until SUBCHUNK_SIZE) {
                val pIdx = bitArray[i]
                result[i] = palette.getOrElse(pIdx) { BedrockBlockState.AIR }
            }

            result
        } catch (e: Exception) {
            null
        }
    }

    private fun nbtToBedrockState(tag: CompoundTag): BedrockBlockState {
        val name = tag.getString("name").ifEmpty { "minecraft:air" }
        val version = tag.getInt("version").let { if (it != 0) it else 18090752 }
        val statesMap = mutableMapOf<String, Any>()

        val statesComp = tag.getCompoundTag("states")
        if (statesComp != null) {
            for ((k, v) in statesComp) {
                when (v) {
                    is ByteTag -> statesMap[k] = (v.asByte() != 0.toByte())
                    is IntTag -> statesMap[k] = v.asInt()
                    is StringTag -> statesMap[k] = v.valueToString().replace("\"", "")
                    else -> statesMap[k] = v.valueToString().replace("\"", "")
                }
            }
        }

        return BedrockBlockState(name, statesMap, version)
    }
}
