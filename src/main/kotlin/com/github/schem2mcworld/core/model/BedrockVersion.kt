package com.github.schem2mcworld.core.model

enum class BedrockVersion(
    val displayName: String,
    val blockVersionTag: Int,
    val storageVersion: Byte,
    val minY: Int,
    val maxY: Int,
    val minSubChunkY: Int,
    val maxSubChunkY: Int,
    val defaultFlatBottomY: Int
) {
    LEGACY_1_12_TO_1_17(
        displayName = "1.12 - 1.17 (Legacy Height 0..255)",
        blockVersionTag = 17825806,
        storageVersion = 8,
        minY = 0,
        maxY = 255,
        minSubChunkY = 0,
        maxSubChunkY = 15,
        defaultFlatBottomY = 0
    ),

    V1_18(
        displayName = "1.18 - 1.19 (Extended Height -64..319)",
        blockVersionTag = 17959425,
        storageVersion = 8,
        minY = -64,
        maxY = 319,
        minSubChunkY = -4,
        maxSubChunkY = 19,
        defaultFlatBottomY = -64
    ),

    V1_20(
        displayName = "1.20",
        blockVersionTag = 17959425,
        storageVersion = 8,
        minY = -64,
        maxY = 319,
        minSubChunkY = -4,
        maxSubChunkY = 19,
        defaultFlatBottomY = -64
    ),

    V1_21(
        displayName = "1.21+",
        blockVersionTag = 18090752,
        storageVersion = 8,
        minY = -64,
        maxY = 319,
        minSubChunkY = -4,
        maxSubChunkY = 19,
        defaultFlatBottomY = -64
    );

    val isExtendedHeight: Boolean get() = minY < 0

    companion object {
        val LATEST = V1_21

        fun fromString(versionStr: String): BedrockVersion {
            val lower = versionStr.lowercase().trim()
            return when {
                lower.contains("legacy") || lower.contains("1.12") || lower.contains("1.16") || lower.contains("1.17") || lower == "old" -> LEGACY_1_12_TO_1_17
                lower.contains("1.18") || lower.contains("1.19") -> V1_18
                lower.contains("1.20") -> V1_20
                lower.contains("1.21") || lower == "latest" -> V1_21
                else -> LATEST
            }
        }
    }
}
