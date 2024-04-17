package io.novafoundation.nova.runtime.network.rpc

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


class SystemPropertiesRequest : RuntimeRequest("system_properties", listOf())


class SystemProperties(
    val sss58Format: Int,
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
            context.deserialize<Any?>(json, valueType)
        }
    }
}
