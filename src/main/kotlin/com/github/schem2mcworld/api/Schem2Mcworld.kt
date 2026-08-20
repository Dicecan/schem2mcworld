package com.github.schem2mcworld.api

import com.github.schem2mcworld.core.mapper.ModFilterMode
import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.model.BedrockVersion
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object Schem2Mcworld {

    @JvmStatic
    @JvmOverloads
    fun convert(
        source: File,
        destination: File,
        worldName: String = source.nameWithoutExtension,
        terrain: WorldTerrainType = WorldTerrainType.SUPERFLAT,
        alignment: AlignmentMode = AlignmentMode.GROUND_ALIGNED,
        targetVersion: BedrockVersion = BedrockVersion.V1_21,
        modFilterMode: ModFilterMode = ModFilterMode.REPLACE_WITH_FALLBACK,
        fallbackBlock: BedrockBlockState = BedrockBlockState.STONE
    ): ConversionResult {
        return McworldConverter.builder()
            .source(source)
            .worldName(worldName)
            .terrain(terrain)
            .alignment(alignment)
            .targetVersion(targetVersion)
            .modFilterMode(modFilterMode)
            .fallbackBlock(fallbackBlock)
            .convert(destination)
    }

    @JvmStatic
    @JvmOverloads
    fun convert(
        source: InputStream,
        destination: OutputStream,
        worldName: String = "Imported World",
        terrain: WorldTerrainType = WorldTerrainType.SUPERFLAT,
        alignment: AlignmentMode = AlignmentMode.GROUND_ALIGNED,
        targetVersion: BedrockVersion = BedrockVersion.V1_21,
        modFilterMode: ModFilterMode = ModFilterMode.REPLACE_WITH_FALLBACK,
        fallbackBlock: BedrockBlockState = BedrockBlockState.STONE
    ): ConversionResult {
        return McworldConverter.builder()
            .source(source)
            .worldName(worldName)
            .terrain(terrain)
            .alignment(alignment)
            .targetVersion(targetVersion)
            .modFilterMode(modFilterMode)
            .fallbackBlock(fallbackBlock)
            .convert(destination)
    }

    @JvmStatic
    @JvmOverloads
    fun convert(
        schematicBytes: ByteArray,
        worldName: String = "Imported World",
        terrain: WorldTerrainType = WorldTerrainType.SUPERFLAT,
        alignment: AlignmentMode = AlignmentMode.GROUND_ALIGNED,
        targetVersion: BedrockVersion = BedrockVersion.V1_21
    ): ByteArray {
        val input = ByteArrayInputStream(schematicBytes)
        val output = ByteArrayOutputStream()
        convert(input, output, worldName = worldName, terrain = terrain, alignment = alignment, targetVersion = targetVersion)
        return output.toByteArray()
    }

    @JvmStatic
    @JvmOverloads
    fun importIntoWorld(
        baseWorld: File,
        schematics: List<SchematicPlacement>,
        destination: File = baseWorld,
        targetVersion: BedrockVersion = BedrockVersion.V1_21,
        modFilterMode: ModFilterMode = ModFilterMode.REPLACE_WITH_FALLBACK
    ): ConversionResult {
        val builder = McworldConverter.builder()
            .baseWorld(baseWorld)
            .targetVersion(targetVersion)
            .modFilterMode(modFilterMode)

        for (placement in schematics) {
            builder.addSchematic(placement)
        }

        return builder.convert(destination)
    }
}

fun File.convertToMcworld(
    destination: File,
    options: ConversionOptions = ConversionOptions()
): ConversionResult = McworldConverter.convert(this, destination, options)

fun InputStream.convertToMcworld(
    destination: OutputStream,
    options: ConversionOptions = ConversionOptions()
): ConversionResult = McworldConverter.convert(this, destination, options)
