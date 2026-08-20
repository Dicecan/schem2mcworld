package com.github.schem2mcworld.core.parser

import com.github.schem2mcworld.core.model.SchematicData
import com.github.schem2mcworld.core.util.NbtHelper
import net.querz.nbt.tag.ByteArrayTag
import net.querz.nbt.tag.CompoundTag
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

object ParserFactory {

    val legacyParser = LegacySchematicParser()
    val spongeParser = SpongeSchematicParser()
    val litematicaParser = LitematicaSchematicParser()

    fun parse(inputStream: InputStream): SchematicData {
        val bytes = inputStream.readBytes()
        val format = detectFormat(bytes)

        val parser: SchematicParser = when (format) {
            SchematicFormat.LITEMATICA_SCHEMATIC -> litematicaParser
            SchematicFormat.SPONGE_SCHEMATIC -> spongeParser
            SchematicFormat.LEGACY_SCHEMATIC -> legacyParser
            SchematicFormat.UNKNOWN -> {
                try {
                    return litematicaParser.parse(ByteArrayInputStream(bytes))
                } catch (e1: Exception) {
                    try {
                        return spongeParser.parse(ByteArrayInputStream(bytes))
                    } catch (e2: Exception) {
                        legacyParser
                    }
                }
            }
        }

        return parser.parse(ByteArrayInputStream(bytes))
    }

    fun parse(file: File): SchematicData {
        val ext = file.extension.lowercase()
        return file.inputStream().use { stream ->
            when (ext) {
                "litematic" -> litematicaParser.parse(stream)
                "schem" -> spongeParser.parse(stream)
                "schematic" -> legacyParser.parse(stream)
                else -> parse(stream)
            }
        }
    }

    fun detectFormat(bytes: ByteArray): SchematicFormat {
        return try {
            val root = NbtHelper.readCompoundTag(ByteArrayInputStream(bytes))

            val blocksTag = root.get("Blocks")
            val paletteTag = root.get("Palette")
            val blockDataTag = root.get("BlockData")
            val schematicTag = root.get("Schematic") as? CompoundTag
            val regionsTag = root.get("Regions")

            if (regionsTag is CompoundTag && (root.containsKey("Version") || root.containsKey("Metadata") || root.containsKey("MinecraftDataVersion") || regionsTag.size() > 0)) {
                SchematicFormat.LITEMATICA_SCHEMATIC
            } else if (blocksTag is ByteArrayTag && (root.containsKey("Data") || root.containsKey("Materials") || root.containsKey("Width"))) {
                SchematicFormat.LEGACY_SCHEMATIC
            } else if (paletteTag != null || blockDataTag != null ||
                (schematicTag?.containsKey("Palette") == true) ||
                (blocksTag is CompoundTag && blocksTag.containsKey("Palette"))
            ) {
                SchematicFormat.SPONGE_SCHEMATIC
            } else {
                SchematicFormat.UNKNOWN
            }
        } catch (e: Exception) {
            SchematicFormat.UNKNOWN
        }
    }
}
