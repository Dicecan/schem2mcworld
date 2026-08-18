package com.github.schem2mcworld.core.writer

import com.github.schem2mcworld.api.WorldTerrainType
import com.github.schem2mcworld.core.model.BedrockVersion
import com.github.schem2mcworld.core.terrain.WorldTerrainGenerator
import com.github.schem2mcworld.core.util.LittleEndianNbtUtil
import net.querz.nbt.tag.ByteTag
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.IntTag
import net.querz.nbt.tag.LongTag
import net.querz.nbt.tag.StringTag
import java.io.File

class WorldTemplateManager {

    fun createLevelDatCompound(
        worldName: String,
        spawnX: Int = 0,
        spawnY: Int = 64,
        spawnZ: Int = 0,
        terrain: WorldTerrainGenerator,
        targetVersion: BedrockVersion = BedrockVersion.LATEST
    ): CompoundTag {
        val root = CompoundTag()

        root.put("LevelName", StringTag(worldName))
        root.put("StorageVersion", IntTag(if (targetVersion == BedrockVersion.LEGACY_1_12_TO_1_17) 8 else 10))
        root.put("NetworkVersion", IntTag(0))
        root.put("Platform", IntTag(2))
        root.put("PlatformBroadcastIntent", IntTag(3))

        val generatorType = terrain.terrainType.generatorId
        root.put("Generator", IntTag(generatorType))

        if (terrain.terrainType == WorldTerrainType.OLD_LIMITED) {
            root.put("LimitedWorldWidth", IntTag(256))
            root.put("LimitedWorldDepth", IntTag(256))
            root.put("LimitedWorldOriginX", IntTag(0))
            root.put("LimitedWorldOriginY", IntTag(0))
            root.put("LimitedWorldOriginZ", IntTag(0))
        }

        root.put("GameType", IntTag(1))
        root.put("Difficulty", IntTag(1))
        root.put("SpawnX", IntTag(spawnX))
        root.put("SpawnY", IntTag(spawnY))
        root.put("SpawnZ", IntTag(spawnZ))
        root.put("RandomSeed", LongTag(123456789L))
        root.put("Time", LongTag(1000L))
        root.put("DayCycleStopTime", LongTag(-1L))
        root.put("LastPlayed", LongTag(System.currentTimeMillis() / 1000L))
        root.put("commandsEnabled", ByteTag(1))
        root.put("hasBeenLoadedInCreative", ByteTag(1))
        root.put("immutableWorld", ByteTag(0))
        root.put("MultiplayerGame", ByteTag(1))
        root.put("LANBroadcast", ByteTag(1))
        root.put("XBLBroadcastIntent", IntTag(3))

        if (generatorType == 2) {
            val flatLayersJson = buildFlatWorldLayersJson(terrain, targetVersion)
            root.put("FlatWorldLayers", StringTag(flatLayersJson))
        }

        val experimentsTag = CompoundTag()
        experimentsTag.put("experiments_ever_used", ByteTag(0))
        experimentsTag.put("saved_with_toggled_experiments", ByteTag(0))
        root.put("experiments", experimentsTag)

        return root
    }

    fun writeLevelDat(
        file: File,
        worldName: String,
        spawnX: Int = 0,
        spawnY: Int = 64,
        spawnZ: Int = 0,
        terrain: WorldTerrainGenerator,
        targetVersion: BedrockVersion = BedrockVersion.LATEST
    ) {
        val compound = createLevelDatCompound(worldName, spawnX, spawnY, spawnZ, terrain, targetVersion)
        file.outputStream().use { stream ->
            val storageVer = if (targetVersion == BedrockVersion.LEGACY_1_12_TO_1_17) 8 else 10
            LittleEndianNbtUtil.writeLevelDat(compound, stream, storageVersion = storageVer)
        }
    }

    fun writeLevelName(file: File, worldName: String) {
        file.writeText(worldName, Charsets.UTF_8)
    }

    fun writeWorldIcon(file: File) {
        val minimalJpeg = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xDB.toByte(), 0x00, 0x43, 0x00, 0x08,
            0x06, 0x06, 0x07, 0x06, 0x05, 0x08, 0x07, 0x07, 0x07, 0x09, 0x09, 0x08,
            0x0A, 0x0C, 0x14, 0x0D, 0x0C, 0x0B, 0x0B, 0x0C, 0x19, 0x12, 0x13, 0x0F,
            0x14, 0x1D, 0x1A, 0x1F, 0x1E, 0x1D, 0x1A, 0x1C, 0x1C, 0x20, 0x24, 0x2E,
            0x27, 0x20, 0x22, 0x2C, 0x23, 0x1C, 0x1C, 0x28, 0x37, 0x29, 0x2C, 0x30,
            0x31, 0x34, 0x34, 0x34, 0x1F, 0x27, 0x39, 0x3D, 0x38, 0x32, 0x3C, 0x2E,
            0x33, 0x34, 0x32, 0xFF.toByte(), 0xC0.toByte(), 0x00, 0x0B, 0x08, 0x00, 0x01, 0x00,
            0x01, 0x01, 0x01, 0x11, 0x00, 0xFF.toByte(), 0xC4.toByte(), 0x00, 0x1F, 0x00, 0x00,
            0x01, 0x05, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x09, 0x0A, 0x0B, 0xFF.toByte(), 0xDA.toByte(), 0x00, 0x08, 0x01, 0x01, 0x00, 0x00,
            0x3F, 0x00, 0xBF.toByte(), 0x00, 0xFF.toByte(), 0xD9.toByte()
        )
        file.writeBytes(minimalJpeg)
    }

    private fun buildFlatWorldLayersJson(terrain: WorldTerrainGenerator, targetVersion: BedrockVersion): String {
        val layers = terrain.effectiveLayers
        val versionSuffix = if (targetVersion.isExtendedHeight) ",\"world_version\":\"version.post_1_18\"" else ""

        if (layers.isEmpty()) {
            return "{\"biome_id\":1,\"block_layers\":[{\"block_name\":\"minecraft:air\",\"count\":1}],\"encoding_version\":6,\"structure_options\":null$versionSuffix}"
        }

        val layerStrings = layers.joinToString(",") { layer ->
            "{\"block_name\":\"${layer.block.name}\",\"count\":${layer.count}}"
        }
        return "{\"biome_id\":1,\"block_layers\":[$layerStrings],\"encoding_version\":6,\"structure_options\":null$versionSuffix}"
    }
}
