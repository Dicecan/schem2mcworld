package com.github.schem2mcworld.api

import com.github.schem2mcworld.core.mapper.BlockStateMapper
import com.github.schem2mcworld.core.mapper.ModFilterPolicy
import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.model.BedrockVersion
import com.github.schem2mcworld.core.model.Vector3i
import java.io.File

data class ConversionOptions(
    val worldName: String = "Imported Schematic",
    val terrainType: WorldTerrainType = WorldTerrainType.VOID,
    val customLayers: List<WorldLayer> = emptyList(),
    val targetPosition: Vector3i = Vector3i(0, 64, 0),
    val alignment: AlignmentMode = AlignmentMode.ABSOLUTE,
    val targetVersion: BedrockVersion = BedrockVersion.LATEST,
    val fallbackBlock: BedrockBlockState = BedrockBlockState.STONE,
    val modFilterPolicy: ModFilterPolicy = ModFilterPolicy.REPLACE_WITH_FALLBACK,
    val customMapper: BlockStateMapper? = null,
    val baseWorld: File? = null,
    val placements: List<SchematicPlacement> = emptyList(),
    val pasteAir: Boolean = false
)
