package com.github.schem2mcworld.api

data class ConversionResult(
    val success: Boolean,
    val worldName: String,
    val totalBlocksConverted: Long,
    val chunksGenerated: Int,
    val subChunksWritten: Int,
    val durationMs: Long,
    val unmappedBlocks: Set<String>,
    val outputSizeBytes: Long
)
