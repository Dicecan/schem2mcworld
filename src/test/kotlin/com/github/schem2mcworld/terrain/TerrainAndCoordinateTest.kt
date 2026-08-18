package com.github.schem2mcworld.terrain

import com.github.schem2mcworld.api.AlignmentMode
import com.github.schem2mcworld.api.WorldLayer
import com.github.schem2mcworld.api.WorldTerrainType
import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.model.SchematicData
import com.github.schem2mcworld.core.model.UniversalBlock
import com.github.schem2mcworld.core.model.Vector3i
import com.github.schem2mcworld.core.terrain.CoordinateTransformer
import com.github.schem2mcworld.core.terrain.WorldTerrainGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TerrainAndCoordinateTest {

    @Test
    fun `test WorldTerrainGenerator layer calculations`() {
        // 1. VOID
        val voidTerrain = WorldTerrainGenerator(WorldTerrainType.VOID)
        assertEquals(0, voidTerrain.effectiveLayers.size)
        assertEquals(0, voidTerrain.totalHeight)
        assertNull(voidTerrain.getBlockAtY(0))

        // 2. SUPERFLAT
        val flatTerrain = WorldTerrainGenerator(WorldTerrainType.SUPERFLAT, bottomY = -64)
        assertEquals(3, flatTerrain.effectiveLayers.size)
        assertEquals(4, flatTerrain.totalHeight)
        assertEquals(-60, flatTerrain.surfaceY) // -64 + 4 = -60
        assertEquals(BedrockBlockState.BEDROCK, flatTerrain.getBlockAtY(-64))
        assertEquals(BedrockBlockState.DIRT, flatTerrain.getBlockAtY(-63))
        assertEquals(BedrockBlockState.DIRT, flatTerrain.getBlockAtY(-62))
        assertEquals(BedrockBlockState.GRASS_BLOCK, flatTerrain.getBlockAtY(-61))
        assertNull(flatTerrain.getBlockAtY(-60))

        // 3. SUPERFLAT_OCEAN
        val oceanTerrain = WorldTerrainGenerator(WorldTerrainType.SUPERFLAT_OCEAN, bottomY = -64)
        assertEquals(4, oceanTerrain.effectiveLayers.size)
        assertEquals(36, oceanTerrain.totalHeight)
        assertEquals(-28, oceanTerrain.surfaceY)
        assertEquals(BedrockBlockState.WATER, oceanTerrain.getBlockAtY(-30))

        // 4. CUSTOM
        val customTerrain = WorldTerrainGenerator(
            terrainType = WorldTerrainType.CUSTOM,
            customLayers = listOf(
                WorldLayer(BedrockBlockState.BEDROCK, 1),
                WorldLayer(BedrockBlockState.STONE, 9)
            ),
            bottomY = 0
        )
        assertEquals(10, customTerrain.totalHeight)
        assertEquals(10, customTerrain.surfaceY)
        assertEquals(BedrockBlockState.STONE, customTerrain.getBlockAtY(5))
    }

    @Test
    fun `test CoordinateTransformer placement calculations`() {
        val dummySchematic = SchematicData(
            width = 10,
            height = 20,
            length = 30,
            blocks = Array(10 * 20 * 30) { UniversalBlock.AIR }
        )

        val terrain = WorldTerrainGenerator(WorldTerrainType.SUPERFLAT, bottomY = -64)

        // 1. ABSOLUTE
        val absBounds = CoordinateTransformer.calculatePlacement(
            schematic = dummySchematic,
            targetPosition = Vector3i(100, 50, 200),
            alignment = AlignmentMode.ABSOLUTE,
            terrain = terrain
        )
        assertEquals(100, absBounds.origin.x)
        assertEquals(50, absBounds.origin.y)
        assertEquals(200, absBounds.origin.z)
        assertEquals(100 + 10 - 1, absBounds.maxX)
        assertEquals(50 + 20 - 1, absBounds.maxY)
        assertEquals(200 + 30 - 1, absBounds.maxZ)

        // 2. GROUND_ALIGNED (surfaceY = -60, targetY = 0 -> originY = -60)
        val groundBounds = CoordinateTransformer.calculatePlacement(
            schematic = dummySchematic,
            targetPosition = Vector3i(0, 0, 0),
            alignment = AlignmentMode.GROUND_ALIGNED,
            terrain = terrain
        )
        assertEquals(-60, groundBounds.origin.y)

        // 3. CENTERED (centered at 0, 0)
        val centerBounds = CoordinateTransformer.calculatePlacement(
            schematic = dummySchematic,
            targetPosition = Vector3i(0, 10, 0),
            alignment = AlignmentMode.CENTERED,
            terrain = terrain
        )
        assertEquals(-5, centerBounds.origin.x)
        assertEquals(-15, centerBounds.origin.z)
    }

    @Test
    fun `test legacy Bedrock version pre-1_18 flat world starts at Y=0`() {
        val legacyFlat = WorldTerrainGenerator(
            terrainType = WorldTerrainType.SUPERFLAT,
            targetVersion = com.github.schem2mcworld.core.model.BedrockVersion.LEGACY_1_12_TO_1_17
        )
        assertEquals(0, legacyFlat.bottomY)
        assertEquals(4, legacyFlat.surfaceY) // 0 + 4 = 4
        assertEquals(BedrockBlockState.BEDROCK, legacyFlat.getBlockAtY(0))
        assertEquals(BedrockBlockState.GRASS_BLOCK, legacyFlat.getBlockAtY(3))
        assertNull(legacyFlat.getBlockAtY(4))
    }
}
