package com.github.schem2mcworld.core.terrain

import com.github.schem2mcworld.api.AlignmentMode
import com.github.schem2mcworld.core.model.BedrockVersion
import com.github.schem2mcworld.core.model.SchematicData
import com.github.schem2mcworld.core.model.Vector3i
import org.slf4j.LoggerFactory
import kotlin.math.floor

data class WorldPlacementBounds(
    val origin: Vector3i,
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int,
    val minZ: Int,
    val maxZ: Int,
    val minChunkX: Int,
    val maxChunkX: Int,
    val minChunkZ: Int,
    val maxChunkZ: Int,
    val minSubChunkY: Int,
    val maxSubChunkY: Int
) {
    fun contains(x: Int, y: Int, z: Int): Boolean =
        x in minX..maxX && y in minY..maxY && z in minZ..maxZ

    fun toLocalPos(x: Int, y: Int, z: Int): Vector3i =
        Vector3i(x - origin.x, y - origin.y, z - origin.z)
}

object CoordinateTransformer {

    private val logger = LoggerFactory.getLogger(CoordinateTransformer::class.java)

    fun calculatePlacement(
        schematic: SchematicData,
        targetPosition: Vector3i,
        alignment: AlignmentMode,
        terrain: WorldTerrainGenerator,
        targetVersion: BedrockVersion = BedrockVersion.LATEST
    ): WorldPlacementBounds {
        val originX: Int
        val originY: Int
        val originZ: Int

        when (alignment) {
            AlignmentMode.ABSOLUTE -> {
                originX = targetPosition.x
                originY = targetPosition.y
                originZ = targetPosition.z
            }
            AlignmentMode.GROUND_ALIGNED -> {
                originX = targetPosition.x
                originY = terrain.surfaceY + targetPosition.y
                originZ = targetPosition.z
            }
            AlignmentMode.CENTERED -> {
                originX = -(schematic.width / 2)
                originY = if (terrain.effectiveLayers.isNotEmpty()) {
                    terrain.surfaceY + targetPosition.y
                } else {
                    targetPosition.y
                }
                originZ = -(schematic.length / 2)
            }
        }

        val minX = originX
        val maxX = originX + schematic.width - 1
        val minY = originY
        val maxY = originY + schematic.height - 1
        val minZ = originZ
        val maxZ = originZ + schematic.length - 1

        val worldMinY = targetVersion.minY
        val worldMaxY = targetVersion.maxY
        val worldMinSubY = targetVersion.minSubChunkY
        val worldMaxSubY = targetVersion.maxSubChunkY

        if (minY < worldMinY || maxY > worldMaxY) {
            logger.warn(
                "Schematic placement Y range [{}, {}] exceeds Bedrock ({}) limits [{}, {}]",
                minY, maxY, targetVersion.displayName, worldMinY, worldMaxY
            )
        }

        val minChunkX = floor(minX / 16.0).toInt()
        val maxChunkX = floor(maxX / 16.0).toInt()
        val minChunkZ = floor(minZ / 16.0).toInt()
        val maxChunkZ = floor(maxZ / 16.0).toInt()

        val effectiveMinY = if (terrain.effectiveLayers.isNotEmpty()) {
            minOf(minY, terrain.bottomY)
        } else minY

        val effectiveMaxY = if (terrain.effectiveLayers.isNotEmpty()) {
            maxOf(maxY, terrain.bottomY + terrain.totalHeight - 1)
        } else maxY

        val minSubChunkY = floor(effectiveMinY / 16.0).toInt().coerceIn(worldMinSubY, worldMaxSubY)
        val maxSubChunkY = floor(effectiveMaxY / 16.0).toInt().coerceIn(worldMinSubY, worldMaxSubY)

        return WorldPlacementBounds(
            origin = Vector3i(originX, originY, originZ),
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            minZ = minZ,
            maxZ = maxZ,
            minChunkX = minChunkX,
            maxChunkX = maxChunkX,
            minChunkZ = minChunkZ,
            maxChunkZ = maxChunkZ,
            minSubChunkY = minSubChunkY,
            maxSubChunkY = maxSubChunkY
        )
    }
}
