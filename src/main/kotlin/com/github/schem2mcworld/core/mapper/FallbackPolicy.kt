package com.github.schem2mcworld.core.mapper

import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.model.UniversalBlock

fun interface FallbackPolicy {
    fun resolve(unmappedBlock: UniversalBlock): BedrockBlockState

    companion object {
        val DEFAULT_STONE = FallbackPolicy { BedrockBlockState.STONE }
        val AIR = FallbackPolicy { BedrockBlockState.AIR }

        fun of(fallbackBlock: BedrockBlockState): FallbackPolicy =
            FallbackPolicy { fallbackBlock }

        val SMART = FallbackPolicy { block ->
            val id = block.id.lowercase()
            when {
                id.contains("air") -> BedrockBlockState.AIR
                id.contains("stairs") -> BedrockBlockState("minecraft:oak_stairs")
                id.contains("slab") -> BedrockBlockState("minecraft:stone_block_slab")
                id.contains("door") -> BedrockBlockState("minecraft:wooden_door")
                id.contains("fence") -> BedrockBlockState("minecraft:fence")
                id.contains("leaves") -> BedrockBlockState("minecraft:leaves")
                id.contains("log") || id.contains("wood") -> BedrockBlockState("minecraft:log")
                id.contains("planks") -> BedrockBlockState("minecraft:planks")
                id.contains("glass") -> BedrockBlockState("minecraft:glass")
                id.contains("wool") -> BedrockBlockState("minecraft:wool")
                id.contains("concrete") -> BedrockBlockState("minecraft:concrete")
                id.contains("terracotta") -> BedrockBlockState("minecraft:stained_hardened_clay")
                id.contains("ore") -> BedrockBlockState("minecraft:iron_ore")
                id.contains("sand") -> BedrockBlockState.SAND
                id.contains("dirt") -> BedrockBlockState.DIRT
                id.contains("water") -> BedrockBlockState.WATER
                else -> BedrockBlockState.STONE
            }
        }
    }
}
