package com.github.schem2mcworld.api

import com.github.schem2mcworld.core.model.Vector3i
import java.io.File
import java.io.InputStream

data class SchematicPlacement(
    val source: Any,
    val position: Vector3i = Vector3i.ZERO,
    val alignment: AlignmentMode = AlignmentMode.ABSOLUTE,
    val pasteAir: Boolean = false
) {
    init {
        require(source is File || source is InputStream || source is com.github.schem2mcworld.core.model.SchematicData) {
            "source must be File, InputStream, or SchematicData, but got ${source.javaClass.name}"
        }
    }
}
