package com.github.schem2mcworld.core.util

import net.querz.nbt.io.LittleEndianNBTInputStream
import net.querz.nbt.io.LittleEndianNBTOutputStream
import net.querz.nbt.io.NamedTag
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.Tag
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

object LittleEndianNbtUtil {

    fun writeTagLE(tag: Tag<*>, outputStream: OutputStream) {
        val nbtOut = LittleEndianNBTOutputStream(outputStream)
        nbtOut.writeTag(NamedTag("", tag), Tag.DEFAULT_MAX_DEPTH)
    }

    fun tagToBytesLE(tag: Tag<*>): ByteArray {
        val baos = ByteArrayOutputStream()
        writeTagLE(tag, baos)
        return baos.toByteArray()
    }

    fun readTagLE(inputStream: InputStream): Tag<*> {
        val nbtIn = LittleEndianNBTInputStream(inputStream)
        val namedTag = nbtIn.readTag(Tag.DEFAULT_MAX_DEPTH)
        return namedTag.tag
    }

    fun bytesToCompoundLE(bytes: ByteArray): CompoundTag {
        val bais = ByteArrayInputStream(bytes)
        return readTagLE(bais) as CompoundTag
    }

    fun writeLevelDat(rootTag: CompoundTag, outputStream: OutputStream, storageVersion: Int = 10) {
        val nbtBytes = tagToBytesLE(rootTag)

        outputStream.write(storageVersion and 0xFF)
        outputStream.write((storageVersion ushr 8) and 0xFF)
        outputStream.write((storageVersion ushr 16) and 0xFF)
        outputStream.write((storageVersion ushr 24) and 0xFF)

        val len = nbtBytes.size
        outputStream.write(len and 0xFF)
        outputStream.write((len ushr 8) and 0xFF)
        outputStream.write((len ushr 16) and 0xFF)
        outputStream.write((len ushr 24) and 0xFF)

        outputStream.write(nbtBytes)
    }

    fun readLevelDat(inputStream: InputStream): Pair<Int, CompoundTag> {
        val header = ByteArray(8)
        var totalRead = 0
        while (totalRead < 8) {
            val r = inputStream.read(header, totalRead, 8 - totalRead)
            if (r == -1) break
            totalRead += r
        }
        val storageVersion = (header[0].toInt() and 0xFF) or
                ((header[1].toInt() and 0xFF) shl 8) or
                ((header[2].toInt() and 0xFF) shl 16) or
                ((header[3].toInt() and 0xFF) shl 24)

        val payload = inputStream.readBytes()
        val tag = bytesToCompoundLE(payload)
        return Pair(storageVersion, tag)
    }
}
