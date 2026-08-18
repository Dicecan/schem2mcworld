package com.github.schem2mcworld.core.writer

import com.github.schem2mcworld.core.model.BlockEntity
import com.github.schem2mcworld.core.util.LittleEndianNbtUtil
import java.io.ByteArrayOutputStream

object LevelDbHelper {

    const val TAG_SUBCHUNK_PREFIX: Byte = 0x2F
    const val TAG_DATA_2D: Byte = 0x2D
    const val TAG_VERSION: Byte = 0x76
    const val TAG_BLOCK_ENTITY: Byte = 0x31

    const val OVERWORLD_VERSION_VALUE: Byte = 40

    fun createSubChunkKey(chunkX: Int, chunkZ: Int, subChunkY: Int): ByteArray {
        val key = ByteArray(9)
        writeIntLE(key, 0, chunkX)
        writeIntLE(key, 4, chunkZ)
        key[8] = TAG_SUBCHUNK_PREFIX
        return key + byteArrayOf(subChunkY.toByte())
    }

    fun createChunkVersionKey(chunkX: Int, chunkZ: Int): ByteArray {
        val key = ByteArray(9)
        writeIntLE(key, 0, chunkX)
        writeIntLE(key, 4, chunkZ)
        key[8] = TAG_VERSION
        return key
    }

    fun create2DDataKey(chunkX: Int, chunkZ: Int): ByteArray {
        val key = ByteArray(9)
        writeIntLE(key, 0, chunkX)
        writeIntLE(key, 4, chunkZ)
        key[8] = TAG_DATA_2D
        return key
    }

    fun createBlockEntityKey(chunkX: Int, chunkZ: Int): ByteArray {
        val key = ByteArray(9)
        writeIntLE(key, 0, chunkX)
        writeIntLE(key, 4, chunkZ)
        key[8] = TAG_BLOCK_ENTITY
        return key
    }

    fun createDefault2DData(surfaceHeight: Short = 64): ByteArray {
        val data = ByteArray(512 + 256)

        for (i in 0 until 256) {
            data[i * 2] = (surfaceHeight.toInt() and 0xFF).toByte()
            data[i * 2 + 1] = ((surfaceHeight.toInt() ushr 8) and 0xFF).toByte()
        }

        for (i in 512 until 512 + 256) {
            data[i] = 1
        }

        return data
    }

    fun serializeBlockEntities(entities: List<BlockEntity>): ByteArray {
        if (entities.isEmpty()) return ByteArray(0)
        val baos = ByteArrayOutputStream()
        for (entity in entities) {
            LittleEndianNbtUtil.writeTagLE(entity.data, baos)
        }
        return baos.toByteArray()
    }

    private fun writeIntLE(array: ByteArray, offset: Int, value: Int) {
        array[offset] = (value and 0xFF).toByte()
        array[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        array[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        array[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }
}
