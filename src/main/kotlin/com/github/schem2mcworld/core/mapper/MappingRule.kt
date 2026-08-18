package com.github.schem2mcworld.core.mapper

import com.github.schem2mcworld.core.model.BedrockBlockState
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

@Serializable(with = MappingRuleSerializer::class)
data class MappingRule(
    val bedrockIdentifier: String,
    val states: Map<String, JsonElement> = emptyMap(),
    val version: Int? = null
) {
    fun toBedrockState(defaultVersion: Int = 18090752): BedrockBlockState {
        val parsedStates = mutableMapOf<String, Any>()
        for ((k, v) in states) {
            if (v is JsonPrimitive) {
                val boolVal = v.booleanOrNull
                val intVal = v.intOrNull
                when {
                    boolVal != null && v.isString.not() -> parsedStates[k] = boolVal
                    intVal != null && v.isString.not() -> parsedStates[k] = intVal
                    else -> parsedStates[k] = v.content
                }
            }
        }
        return BedrockBlockState(
            name = bedrockIdentifier,
            states = parsedStates,
            version = version ?: defaultVersion
        )
    }
}

object MappingRuleSerializer : KSerializer<MappingRule> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MappingRule")

    override fun serialize(encoder: Encoder, value: MappingRule) {}

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): MappingRule {
        val jsonDecoder = decoder as? JsonDecoder ?: throw IllegalStateException("Expected JsonDecoder")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        val idElement = jsonObject["bedrock_identifier"]
            ?: jsonObject["bedrock_name"]
            ?: jsonObject["bedrock_id"]
            ?: jsonObject["name"]
            ?: JsonPrimitive("minecraft:stone")

        val identifier = if (idElement is JsonPrimitive) idElement.content else "minecraft:stone"

        val statesElement = jsonObject["states"] ?: jsonObject["bedrock_states"]
        val statesMap: Map<String, JsonElement> = if (statesElement is JsonObject) {
            statesElement
        } else {
            emptyMap()
        }

        val versionElement = jsonObject["version"]
        val version = (versionElement as? JsonPrimitive)?.intOrNull

        return MappingRule(
            bedrockIdentifier = identifier,
            states = statesMap,
            version = version
        )
    }
}
