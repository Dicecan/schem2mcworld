package com.github.schem2mcworld.core.util

import net.querz.nbt.io.NBTInputStream
import net.querz.nbt.io.NamedTag
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.Tag
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

object NbtHelper {

    fun readNamedTag(inputStream: InputStream): NamedTag {
        val bis = BufferedInputStream(inputStream)
        bis.mark(2)
        val header = ByteArray(2)
        val readCount = bis.read(header)
        bis.reset()

        val actualStream = if (readCount == 2 && (header[0].toInt() and 0xFF) == 0x1F && (header[1].toInt() and 0xFF) == 0x8B) {
            GZIPInputStream(bis)
        } else {
            bis
        }

        return NBTInputStream(actualStream).readTag(Tag.DEFAULT_MAX_DEPTH)
    }

    fun readCompoundTag(inputStream: InputStream): CompoundTag {
        val named = readNamedTag(inputStream)
        return named.tag as? CompoundTag
            ?: throw IllegalArgumentException("Root tag is not a CompoundTag (type=${named.tag?.javaClass?.simpleName})")
    }
}
