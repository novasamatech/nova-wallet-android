package io.novafoundation.nova.common.data.network.runtime.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class SystemProperties(
    val ss58Format: Int?,
    val SS58Prefix: Int?,
    @JsonAdapter(WrapToListSerializer::class)
    val tokenDecimals: List<Int>,
    @JsonAdapter(WrapToListSerializer::class)
    val tokenSymbol: List<String>
)

private class WrapToListSerializer : JsonDeserializer<List<*>> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): List<*> {
        val valueType = (typeOfT as ParameterizedType).actualTypeArguments[0]

        if (json.isJsonPrimitive) {
            return listOf(context.deserialize<Any?>(json, valueType))
        }

        return json.asJsonArray.map {
            context.deserialize<Any?>(it, valueType)
        }
    }
}

fun SystemProperties.firstTokenDecimals() = tokenDecimals.first()

fun SystemProperties.firstTokenSymbol() = tokenSymbol.first()
