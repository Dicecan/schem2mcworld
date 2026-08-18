package com.github.schem2mcworld.core.terrain

import com.github.schem2mcworld.api.WorldLayer
import com.github.schem2mcworld.api.WorldTerrainType
import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.model.BedrockVersion

class WorldTerrainGenerator(
    val terrainType: WorldTerrainType = WorldTerrainType.VOID,
    val customLayers: List<WorldLayer> = emptyList(),
    val targetVersion: BedrockVersion = BedrockVersion.LATEST,
    val bottomY: Int = targetVersion.defaultFlatBottomY
) {
    val effectiveLayers: List<WorldLayer> = when (terrainType) {
        WorldTerrainType.VOID -> emptyList()
        WorldTerrainType.INFINITE -> emptyList()
        WorldTerrainType.OLD_LIMITED -> emptyList()
        WorldTerrainType.SUPERFLAT -> listOf(
            WorldLayer(BedrockBlockState.BEDROCK, 1),
            WorldLayer(BedrockBlockState.DIRT, 2),
            WorldLayer(BedrockBlockState.GRASS_BLOCK, 1)
        )
        WorldTerrainType.SUPERFLAT_OCEAN -> listOf(
            WorldLayer(BedrockBlockState.BEDROCK, 1),
            WorldLayer(BedrockBlockState.STONE, 10),
            WorldLayer(BedrockBlockState.SAND, 5),
            WorldLayer(BedrockBlockState.WATER, 20)
        )
        WorldTerrainType.CUSTOM -> customLayers
    }

    val totalHeight: Int = effectiveLayers.sumOf { it.count }

    val surfaceY: Int = when (terrainType) {
        WorldTerrainType.VOID -> 64
        WorldTerrainType.INFINITE -> 64
        WorldTerrainType.OLD_LIMITED -> 64
        WorldTerrainType.SUPERFLAT,
        WorldTerrainType.SUPERFLAT_OCEAN,
        WorldTerrainType.CUSTOM -> if (effectiveLayers.isEmpty()) 64 else bottomY + totalHeight
    }

    fun getBlockAtY(worldY: Int): BedrockBlockState? {
        if (worldY < bottomY || worldY >= bottomY + totalHeight) {
            return null
        }

        var currentY = bottomY
        for (layer in effectiveLayers) {
            val nextY = currentY + layer.count
            if (worldY in currentY until nextY) {
                return layer.block
            }
            currentY = nextY
        }
        return null
    }

    fun subChunkIntersectsTerrain(subChunkY: Int): Boolean {
        if (effectiveLayers.isEmpty()) return false
        val subMinY = subChunkY * 16
        val subMaxY = subMinY + 15
        val terrainMinY = bottomY
        val terrainMaxY = bottomY + totalHeight - 1
        return subMaxY >= terrainMinY && subMinY <= terrainMaxY
    }
}
