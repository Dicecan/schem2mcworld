package com.github.schem2mcworld.core.mapper

import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.model.UniversalBlock

enum class ModFilterMode {
    REPLACE_WITH_FALLBACK,
    REMOVE_TO_AIR,
    STRICT_VANILLA_ONLY,
    CUSTOM
}

fun interface ModFilterPolicy {
    fun filter(block: UniversalBlock, defaultFallback: BedrockBlockState): BedrockBlockState?

    companion object {
        val REPLACE_WITH_FALLBACK = ModFilterPolicy { block, defaultFallback ->
            if (block.namespace != "minecraft") {
                defaultFallback
            } else null
        }

        val REMOVE_TO_AIR = ModFilterPolicy { block, _ ->
            if (block.namespace != "minecraft") {
                BedrockBlockState.AIR
            } else null
        }

        val STRICT_VANILLA_ONLY = ModFilterPolicy { block, _ ->
            if (block.namespace != "minecraft") {
                BedrockBlockState.AIR
            } else null
        }

        fun fromMode(mode: ModFilterMode, customFallback: BedrockBlockState = BedrockBlockState.AIR): ModFilterPolicy {
            return when (mode) {
                ModFilterMode.REPLACE_WITH_FALLBACK -> REPLACE_WITH_FALLBACK
                ModFilterMode.REMOVE_TO_AIR -> REMOVE_TO_AIR
                ModFilterMode.STRICT_VANILLA_ONLY -> STRICT_VANILLA_ONLY
                ModFilterMode.CUSTOM -> ModFilterPolicy { block, _ ->
                    if (block.namespace != "minecraft") customFallback else null
                }
            }
        }
    }
}
