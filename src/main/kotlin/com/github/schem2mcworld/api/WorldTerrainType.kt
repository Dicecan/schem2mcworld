package com.github.schem2mcworld.api

enum class WorldTerrainType(
    val generatorId: Int
) {
    VOID(generatorId = 2),
    SUPERFLAT(generatorId = 2),
    SUPERFLAT_OCEAN(generatorId = 2),
    INFINITE(generatorId = 1),
    OLD_LIMITED(generatorId = 0),
    CUSTOM(generatorId = 2);

    companion object {
        fun fromString(str: String): WorldTerrainType {
            val lower = str.lowercase().trim()
            return when {
                lower == "void" -> VOID
                lower.contains("ocean") -> SUPERFLAT_OCEAN
                lower.contains("flat") || lower.contains("superflat") -> SUPERFLAT
                lower.contains("infinite") || lower.contains("natural") -> INFINITE
                lower.contains("old") || lower.contains("limited") -> OLD_LIMITED
                lower.contains("custom") -> CUSTOM
                else -> VOID
            }
        }
    }
}
