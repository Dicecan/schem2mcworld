package com.github.schem2mcworld.core.mapper

import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.model.BedrockVersion
import com.github.schem2mcworld.core.model.UniversalBlock
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class BlockStateMapper(
    private val rules: Map<String, MappingRule> = emptyMap(),
    val fallbackPolicy: FallbackPolicy = FallbackPolicy.SMART,
    val modFilterPolicy: ModFilterPolicy = ModFilterPolicy.REPLACE_WITH_FALLBACK
) {
    private val logger = LoggerFactory.getLogger(BlockStateMapper::class.java)
    private val mappingCache = ConcurrentHashMap<String, BedrockBlockState>()
    private val unmappedBlocks = ConcurrentHashMap.newKeySet<String>()

    fun getUnmappedBlocks(): Set<String> = unmappedBlocks.toSet()

    fun map(block: UniversalBlock, targetVersion: BedrockVersion = BedrockVersion.LATEST): BedrockBlockState {
        val stateString = block.toStateString()

        return mappingCache.computeIfAbsent(stateString) {
            val filtered = modFilterPolicy.filter(block, fallbackPolicy.resolve(block))
            if (filtered != null) {
                unmappedBlocks.add(stateString)
                return@computeIfAbsent filtered
            }

            rules[stateString]?.let {
                return@computeIfAbsent it.toBedrockState(targetVersion.blockVersionTag)
            }

            rules[block.fullId]?.let {
                return@computeIfAbsent it.toBedrockState(targetVersion.blockVersionTag)
            }

            rules[block.id]?.let {
                return@computeIfAbsent it.toBedrockState(targetVersion.blockVersionTag)
            }

            if (block.id == "air" || block.id == "cave_air" || block.id == "void_air") {
                return@computeIfAbsent BedrockBlockState.AIR
            }

            unmappedBlocks.add(stateString)
            val fallbackState = fallbackPolicy.resolve(block)
            logger.warn("Unmapped block '{}'. Fallback to '{}'", stateString, fallbackState.name)
            fallbackState
        }
    }

    companion object {
        private var defaultInstance: BlockStateMapper? = null

        fun getDefault(): BlockStateMapper {
            return defaultInstance ?: synchronized(this) {
                defaultInstance ?: run {
                    val rules = MappingDataLoader.loadDefaultJavaToBedrock()
                    BlockStateMapper(rules, FallbackPolicy.SMART, ModFilterPolicy.REPLACE_WITH_FALLBACK).also {
                        defaultInstance = it
                    }
                }
            }
        }
    }
}
