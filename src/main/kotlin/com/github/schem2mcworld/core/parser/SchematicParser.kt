package com.github.schem2mcworld.core.parser

import com.github.schem2mcworld.core.model.SchematicData
import java.io.File
import java.io.InputStream

interface SchematicParser {
    fun supports(format: SchematicFormat): Boolean
    fun parse(inputStream: InputStream): SchematicData
    fun parse(file: File): SchematicData = file.inputStream().use { parse(it) }
}
