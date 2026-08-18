package com.github.schem2mcworld.core.model

import net.querz.nbt.tag.ByteTag
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.IntTag
import net.querz.nbt.tag.StringTag

data class BedrockBlockState(
    val name: String,
    val states: Map<String, Any> = emptyMap(),
    val version: Int = 18090752
) {
    fun toNbt(): CompoundTag {
        val root = CompoundTag()
        root.putString("name", name)

        val statesTag = CompoundTag()
        for ((key, value) in states) {
            when (value) {
                is Boolean -> statesTag.put(key, ByteTag((if (value) 1 else 0).toByte()))
                is Byte -> statesTag.put(key, ByteTag(value))
                is Short -> statesTag.put(key, IntTag(value.toInt()))
                is Int -> statesTag.put(key, IntTag(value))
                is Long -> statesTag.put(key, IntTag(value.toInt()))
                is String -> statesTag.put(key, StringTag(value))
                else -> statesTag.put(key, StringTag(value.toString()))
            }
        }
        root.put("states", statesTag)
        root.putInt("version", version)
        return root
    }

    companion object {
        val AIR = BedrockBlockState("minecraft:air")
        val STONE = BedrockBlockState("minecraft:stone")
        val BEDROCK = BedrockBlockState("minecraft:bedrock")
        val DIRT = BedrockBlockState("minecraft:dirt")
        val GRASS_BLOCK = BedrockBlockState("minecraft:grass_block")
        val WATER = BedrockBlockState("minecraft:water")
        val SAND = BedrockBlockState("minecraft:sand")
    }
}
