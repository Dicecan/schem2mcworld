package com.github.schem2mcworld.core.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

object VarIntUtil {

    fun readVarInt(stream: InputStream): Int {
        var numRead = 0
        var result = 0
        var read: Int
        do {
            read = stream.read()
            if (read == -1) {
                throw IllegalStateException("Premature end of stream while reading VarInt")
            }
            val value = read and 0x7F
            result = result or (value shl (7 * numRead))
            numRead++
            if (numRead > 5) {
                throw IllegalArgumentException("VarInt is too big (> 5 bytes)")
            }
        } while ((read and 0x80) != 0)

        return result
    }

    fun writeVarInt(value: Int, stream: OutputStream) {
        var v = value
        do {
            var temp = v and 0x7F
            v = v ushr 7
            if (v != 0) {
                temp = temp or 0x80
            }
            stream.write(temp)
        } while (v != 0)
    }

    fun readVarIntArray(bytes: ByteArray, count: Int): IntArray {
        val stream = ByteArrayInputStream(bytes)
        val result = IntArray(count)
        for (i in 0 until count) {
            result[i] = readVarInt(stream)
        }
        return result
    }

    fun writeVarIntArray(array: IntArray): ByteArray {
        val stream = ByteArrayOutputStream(array.size * 2)
        for (v in array) {
            writeVarInt(v, stream)
        }
        return stream.toByteArray()
    }
}
