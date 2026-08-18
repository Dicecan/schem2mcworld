package com.github.schem2mcworld.writer

import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.util.BitArray
import com.github.schem2mcworld.core.writer.SubChunkEncoder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SubChunkEncoderTest {

    @Test
    fun `test BitArray packing and unpacking for various bitsPerBlock`() {
        for (bits in listOf(1, 2, 3, 4, 5, 6, 8, 16)) {
            val bitArray = BitArray(bits, 4096)
            val maxValue = (1 shl bits) - 1

            // 写入测试数据
            for (i in 0 until 4096) {
                val value = i % (maxValue + 1)
                bitArray[i] = value
            }

            // 读取并验证
            for (i in 0 until 4096) {
                val expected = i % (maxValue + 1)
                assertEquals(expected, bitArray[i], "BitArray mismatch at index $i for bitsPerBlock=$bits")
            }
        }
    }

    @Test
    fun `test SubChunkEncoder encodes single block type with 1 bit per block`() {
        val blocks = Array(4096) { BedrockBlockState.STONE }
        val encoded = SubChunkEncoder.encode(blocks)

        assertNotNull(encoded)
        assertTrue(encoded.isNotEmpty())
        // Version byte = 8
        assertEquals(8.toByte(), encoded[0])
        // Layer count = 1
        assertEquals(1.toByte(), encoded[1])
        // Storage version (bitsPerBlock=1 -> (1 shl 1) | 0 = 2)
        assertEquals(2.toByte(), encoded[2])
    }

    @Test
    fun `test SubChunkEncoder encodes multi-block palette`() {
        val blocks = Array(4096) { i ->
            when (i % 3) {
                0 -> BedrockBlockState.STONE
                1 -> BedrockBlockState.DIRT
                else -> BedrockBlockState.GRASS_BLOCK
            }
        }
        val encoded = SubChunkEncoder.encode(blocks)

        assertNotNull(encoded)
        assertTrue(encoded.size > 100)
        // bitsPerBlock for 3 palette items should be 2 -> storage version byte = (2 shl 1) = 4
        assertEquals(4.toByte(), encoded[2])
    }
}
