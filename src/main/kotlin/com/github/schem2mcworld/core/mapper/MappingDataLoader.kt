package com.github.schem2mcworld.core.mapper

import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.net.URI

object MappingDataLoader {

    private val logger = LoggerFactory.getLogger(MappingDataLoader::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadJavaToBedrock(inputStream: InputStream): Map<String, MappingRule> {
        val text = inputStream.bufferedReader().use { it.readText() }
        return json.decodeFromString<Map<String, MappingRule>>(text)
    }

    fun loadJavaToBedrock(file: File): Map<String, MappingRule> {
        return file.inputStream().use { loadJavaToBedrock(it) }
    }

    fun loadJavaToBedrockFromUrl(url: String): Map<String, MappingRule> {
        logger.info("Loading external mappings from URL: {}", url)
        val stream = URI(url).toURL().openStream()
        return stream.use { loadJavaToBedrock(it) }
    }

    fun loadDefaultJavaToBedrock(): Map<String, MappingRule> {
        val resourceStream = javaClass.getResourceAsStream("/mappings/java_to_bedrock.json")
            ?: throw IllegalStateException("Resource /mappings/java_to_bedrock.json not found")
        return loadJavaToBedrock(resourceStream)
    }

    fun mergeMappings(vararg mappingMaps: Map<String, MappingRule>): Map<String, MappingRule> {
        val merged = mutableMapOf<String, MappingRule>()
        for (map in mappingMaps) {
            merged.putAll(map)
        }
        return merged
    }

    fun loadLegacyToJava(inputStream: InputStream): Map<String, String> {
        val text = inputStream.bufferedReader().use { it.readText() }
        return json.decodeFromString<Map<String, String>>(text)
    }

    fun loadLegacyToJava(file: File): Map<String, String> {
        return file.inputStream().use { loadLegacyToJava(it) }
    }

    fun loadDefaultLegacyToJava(): Map<String, String> {
        val resourceStream = javaClass.getResourceAsStream("/mappings/legacy_to_java.json")
            ?: throw IllegalStateException("Resource /mappings/legacy_to_java.json not found")
        return loadLegacyToJava(resourceStream)
    }
}
