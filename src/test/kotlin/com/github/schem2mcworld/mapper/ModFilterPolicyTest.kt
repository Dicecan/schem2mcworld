package com.github.schem2mcworld.mapper

import com.github.schem2mcworld.core.mapper.BlockStateMapper
import com.github.schem2mcworld.core.mapper.FallbackPolicy
import com.github.schem2mcworld.core.mapper.ModFilterMode
import com.github.schem2mcworld.core.mapper.ModFilterPolicy
import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.model.UniversalBlock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ModFilterPolicyTest {

    @Test
    fun `test remove mod blocks to air`() {
        val modBlock = UniversalBlock("twilightforest", "twilight_oak_stairs")
        val vanillaBlock = UniversalBlock("minecraft", "stone")

        val mapper = BlockStateMapper(
            rules = emptyMap(),
            fallbackPolicy = FallbackPolicy.of(BedrockBlockState.STONE),
            modFilterPolicy = ModFilterPolicy.fromMode(ModFilterMode.REMOVE_TO_AIR)
        )

        val modResult = mapper.map(modBlock)
        assertEquals(BedrockBlockState.AIR.name, modResult.name)

        val vanillaResult = mapper.map(vanillaBlock)
        assertEquals("minecraft:stone", vanillaResult.name)
    }

    @Test
    fun `test replace mod blocks with fallback`() {
        val modBlock = UniversalBlock("botania", "mana_pylon")
        val customFallback = BedrockBlockState("minecraft:gold_block")

        val mapper = BlockStateMapper(
            rules = emptyMap(),
            fallbackPolicy = FallbackPolicy.of(customFallback),
            modFilterPolicy = ModFilterPolicy.fromMode(ModFilterMode.REPLACE_WITH_FALLBACK, customFallback)
        )

        val modResult = mapper.map(modBlock)
        assertEquals("minecraft:gold_block", modResult.name)
    }
}
