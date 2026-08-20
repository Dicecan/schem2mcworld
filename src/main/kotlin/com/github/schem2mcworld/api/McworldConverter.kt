package com.github.schem2mcworld.api

import com.github.schem2mcworld.core.mapper.BlockStateMapper
import com.github.schem2mcworld.core.mapper.FallbackPolicy
import com.github.schem2mcworld.core.mapper.MappingDataLoader
import com.github.schem2mcworld.core.mapper.ModFilterMode
import com.github.schem2mcworld.core.mapper.ModFilterPolicy
import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.model.BedrockVersion
import com.github.schem2mcworld.core.model.SchematicData
import com.github.schem2mcworld.core.model.Vector3i
import com.github.schem2mcworld.core.parser.ParserFactory
import com.github.schem2mcworld.core.writer.McworldWriter
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class McworldConverter private constructor(
    private val sourceFile: File?,
    private val sourceStream: InputStream?,
    private val options: ConversionOptions
) {
    private val writer = McworldWriter()

    fun convert(destinationFile: File): ConversionResult {
        val schematic = parseSchematic()
        return writer.write(schematic, options, destinationFile)
    }

    fun convert(outputStream: OutputStream): ConversionResult {
        val schematic = parseSchematic()
        return writer.writeToStream(schematic, options, outputStream)
    }

    private fun parseSchematic(): SchematicData? {
        return when {
            sourceFile != null -> ParserFactory.parse(sourceFile)
            sourceStream != null -> ParserFactory.parse(sourceStream)
            options.placements.isNotEmpty() -> null
            else -> throw IllegalStateException("Neither source file, input stream, nor schematic placements were provided.")
        }
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()

        @JvmStatic
        fun convert(
            sourceFile: File,
            destinationFile: File,
            options: ConversionOptions = ConversionOptions()
        ): ConversionResult {
            val schematic = ParserFactory.parse(sourceFile)
            return McworldWriter().write(schematic, options, destinationFile)
        }

        @JvmStatic
        fun convert(
            inputStream: InputStream,
            outputStream: OutputStream,
            options: ConversionOptions = ConversionOptions()
        ): ConversionResult {
            val schematic = ParserFactory.parse(inputStream)
            return McworldWriter().writeToStream(schematic, options, outputStream)
        }
    }

    class Builder {
        private var sourceFile: File? = null
        private var sourceStream: InputStream? = null
        private var worldName: String = "Imported Schematic"
        private var terrainType: WorldTerrainType = WorldTerrainType.VOID
        private var customLayers: List<WorldLayer> = emptyList()
        private var targetPosition: Vector3i = Vector3i(0, 64, 0)
        private var alignment: AlignmentMode = AlignmentMode.ABSOLUTE
        private var targetVersion: BedrockVersion = BedrockVersion.LATEST
        private var fallbackBlock: BedrockBlockState = BedrockBlockState.STONE
        private var modFilterPolicy: ModFilterPolicy = ModFilterPolicy.REPLACE_WITH_FALLBACK
        private var customMapper: BlockStateMapper? = null
        private var baseWorld: File? = null
        private val placements = mutableListOf<SchematicPlacement>()
        private var pasteAir: Boolean = false

        fun source(file: File) = apply { this.sourceFile = file }

        fun source(inputStream: InputStream) = apply { this.sourceStream = inputStream }

        fun baseWorld(file: File) = apply { this.baseWorld = file }

        fun addSchematic(
            file: File,
            x: Int = 0,
            y: Int = 64,
            z: Int = 0,
            alignment: AlignmentMode = AlignmentMode.ABSOLUTE,
            pasteAir: Boolean = false
        ) = apply {
            this.placements.add(SchematicPlacement(file, Vector3i(x, y, z), alignment, pasteAir))
        }

        fun addSchematic(
            inputStream: InputStream,
            x: Int = 0,
            y: Int = 64,
            z: Int = 0,
            alignment: AlignmentMode = AlignmentMode.ABSOLUTE,
            pasteAir: Boolean = false
        ) = apply {
            this.placements.add(SchematicPlacement(inputStream, Vector3i(x, y, z), alignment, pasteAir))
        }

        fun addSchematic(placement: SchematicPlacement) = apply {
            this.placements.add(placement)
        }

        fun addSchematics(placementsList: List<SchematicPlacement>) = apply {
            this.placements.addAll(placementsList)
        }

        fun pasteAir(paste: Boolean) = apply { this.pasteAir = paste }

        fun worldName(name: String) = apply { this.worldName = name }

        fun terrain(type: WorldTerrainType) = apply { this.terrainType = type }

        fun customLayers(layers: List<WorldLayer>) = apply {
            this.terrainType = WorldTerrainType.CUSTOM
            this.customLayers = layers
        }

        fun customLayers(vararg layers: WorldLayer) = apply {
            this.terrainType = WorldTerrainType.CUSTOM
            this.customLayers = layers.toList()
        }

        fun targetPosition(x: Int, y: Int, z: Int) = apply {
            this.targetPosition = Vector3i(x, y, z)
        }

        fun targetY(y: Int) = apply {
            this.targetPosition = Vector3i(targetPosition.x, y, targetPosition.z)
        }

        fun alignment(mode: AlignmentMode) = apply { this.alignment = mode }

        fun targetVersion(version: BedrockVersion) = apply { this.targetVersion = version }

        fun fallbackBlock(block: BedrockBlockState) = apply { this.fallbackBlock = block }

        fun addMappingFile(file: File) = apply {
            val loaded = MappingDataLoader.loadJavaToBedrock(file)
            val baseRules = MappingDataLoader.loadDefaultJavaToBedrock()
            val merged = MappingDataLoader.mergeMappings(baseRules, loaded)
            this.customMapper = BlockStateMapper(merged, fallbackPolicy = FallbackPolicy.of(fallbackBlock), modFilterPolicy = modFilterPolicy)
        }

        fun addMappingUrl(url: String) = apply {
            val loaded = MappingDataLoader.loadJavaToBedrockFromUrl(url)
            val baseRules = MappingDataLoader.loadDefaultJavaToBedrock()
            val merged = MappingDataLoader.mergeMappings(baseRules, loaded)
            this.customMapper = BlockStateMapper(merged, fallbackPolicy = FallbackPolicy.of(fallbackBlock), modFilterPolicy = modFilterPolicy)
        }

        fun modFilterMode(mode: ModFilterMode) = apply {
            this.modFilterPolicy = ModFilterPolicy.fromMode(mode, fallbackBlock)
        }

        fun modFilterPolicy(policy: ModFilterPolicy) = apply { this.modFilterPolicy = policy }

        fun removeModBlocks(remove: Boolean = true) = apply {
            if (remove) {
                this.modFilterPolicy = ModFilterPolicy.REMOVE_TO_AIR
            }
        }

        fun customMapper(mapper: BlockStateMapper) = apply { this.customMapper = mapper }

        fun build(): McworldConverter {
            val options = ConversionOptions(
                worldName = worldName,
                terrainType = terrainType,
                customLayers = customLayers,
                targetPosition = targetPosition,
                alignment = alignment,
                targetVersion = targetVersion,
                fallbackBlock = fallbackBlock,
                modFilterPolicy = modFilterPolicy,
                customMapper = customMapper,
                baseWorld = baseWorld,
                placements = placements.toList(),
                pasteAir = pasteAir
            )
            return McworldConverter(sourceFile, sourceStream, options)
        }

        fun convert(destinationFile: File): ConversionResult =
            build().convert(destinationFile)

        fun convert(outputStream: OutputStream): ConversionResult =
            build().convert(outputStream)
    }
}
