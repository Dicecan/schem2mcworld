package com.github.schem2mcworld.mapper

import com.github.schem2mcworld.core.mapper.BlockStateMapper
import com.github.schem2mcworld.core.mapper.FallbackPolicy
import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.model.UniversalBlock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BlockStateMapperTest {

    private val mapper = BlockStateMapper.getDefault()

    @Test
    fun `test standard block mapping`() {
        val stone = UniversalBlock(id = "stone")
        val bedrockState = mapper.map(stone)
        assertEquals("minecraft:stone", bedrockState.name)
        assertEquals("stone", bedrockState.states["stone_type"])
    }

    @Test
    fun `test block state with properties mapping`() {
        val stairs = UniversalBlock(
            id = "oak_stairs",
            properties = mapOf("facing" to "north", "half" to "bottom", "shape" to "straight")
        )
        val bedrockState = mapper.map(stairs)
        assertEquals("minecraft:oak_stairs", bedrockState.name)
        assertEquals(3, bedrockState.states["weirdo_direction"])
        assertEquals(false, bedrockState.states["upside_down_bit"])
    }

    @Test
    fun `test unknown block triggers fallback policy without throwing exception`() {
        val unknownBlock = UniversalBlock(
            namespace = "mysticmod",
            id = "super_unknown_crystal_block",
            properties = mapOf("sparkle" to "true")
        )

        // 默认智能策略应该返回一个有效方块（例如 stone）而不是抛出异常
        val bedrockState = mapper.map(unknownBlock)
        assertNotNull(bedrockState)
        assertEquals("minecraft:stone", bedrockState.name)

        // 检查未识别列表是否正确记录
        assertTrue(mapper.getUnmappedBlocks().contains(unknownBlock.toStateString()))
    }

    @Test
    fun `test custom fallback policy`() {
        val customFallbackMapper = BlockStateMapper(
            rules = emptyMap(),
            fallbackPolicy = FallbackPolicy.of(BedrockBlockState("minecraft:sponge"))
        )

        val unmapped = UniversalBlock(id = "unmapped_test_block")
        val mapped = customFallbackMapper.map(unmapped)
        assertEquals("minecraft:sponge", mapped.name)
    }
}
