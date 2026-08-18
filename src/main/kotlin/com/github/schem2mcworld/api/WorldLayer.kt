package com.github.schem2mcworld.api

import com.github.schem2mcworld.core.model.BedrockBlockState

data class WorldLayer(
    val block: BedrockBlockState,
    val count: Int
) {
    init {
        require(count > 0) { "Layer count must be positive, got $count" }
    }
}
