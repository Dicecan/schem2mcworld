package com.github.schem2mcworld.core.model

import net.querz.nbt.tag.CompoundTag

data class UniversalBlock(
    val namespace: String = "minecraft",
    val id: String,
    val properties: Map<String, String> = emptyMap(),
    val nbtData: CompoundTag? = null
) {
    val fullId: String get() = "$namespace:$id"

    fun toStateString(): String {
        if (properties.isEmpty()) return fullId
        val sortedProps = properties.entries.sortedBy { it.key }
            .joinToString(",") { "${it.key}=${it.value}" }
        return "$fullId[$sortedProps]"
    }

    companion object {
        val AIR = UniversalBlock(id = "air")
        val STONE = UniversalBlock(id = "stone")
        val BEDROCK = UniversalBlock(id = "bedrock")
        val DIRT = UniversalBlock(id = "dirt")
        val GRASS_BLOCK = UniversalBlock(id = "grass_block")
        val WATER = UniversalBlock(id = "water")

        fun fromStateString(stateString: String, nbtData: CompoundTag? = null): UniversalBlock {
            val trimmed = stateString.trim()
            val bracketIndex = trimmed.indexOf('[')

            if (bracketIndex == -1) {
                val parts = trimmed.split(":", limit = 2)
                return if (parts.size == 2) {
                    UniversalBlock(namespace = parts[0], id = parts[1], nbtData = nbtData)
                } else {
                    UniversalBlock(namespace = "minecraft", id = parts[0], nbtData = nbtData)
                }
            }

            val idPart = trimmed.substring(0, bracketIndex)
            val propPart = trimmed.substring(bracketIndex + 1, trimmed.lastIndexOf(']'))
            val parts = idPart.split(":", limit = 2)
            val ns = if (parts.size == 2) parts[0] else "minecraft"
            val id = if (parts.size == 2) parts[1] else parts[0]

            val props = if (propPart.isNotBlank()) {
                propPart.split(",").associate { kvStr ->
                    val kv = kvStr.split("=", limit = 2)
                    kv[0].trim() to kv.getOrElse(1) { "" }.trim()
                }
            } else {
                emptyMap()
            }

            return UniversalBlock(namespace = ns, id = id, properties = props, nbtData = nbtData)
        }
    }
}
