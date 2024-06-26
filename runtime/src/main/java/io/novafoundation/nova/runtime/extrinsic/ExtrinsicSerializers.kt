package io.novafoundation.nova.runtime.extrinsic

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import io.novafoundation.nova.common.utils.ByteArrayHexAdapter
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Extrinsic
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import java.lang.reflect.Type

private class GenericCallAdapter : JsonSerializer<GenericCall.Instance> {

    override fun serialize(src: GenericCall.Instance, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonObject().apply {
            add("module", JsonPrimitive(src.module.name))
            add("function", JsonPrimitive(src.function.name))
            add("args", context.serialize(src.arguments))
        }
    }
}

private class CallRepresentationAdapter : JsonSerializer<Extrinsic.EncodingInstance.CallRepresentation> {

    override fun serialize(src: Extrinsic.EncodingInstance.CallRepresentation, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return when (src) {
            is Extrinsic.EncodingInstance.CallRepresentation.Instance -> context.serialize(src.call)
            is Extrinsic.EncodingInstance.CallRepresentation.Bytes -> context.serialize(src.bytes)
        }
    }
}

object ExtrinsicSerializers {

    fun gson() = GsonBuilder()
        .registerTypeHierarchyAdapter(ByteArray::class.java, ByteArrayHexAdapter())
        .registerTypeHierarchyAdapter(GenericCall.Instance::class.java, GenericCallAdapter())
        .registerTypeHierarchyAdapter(Extrinsic.EncodingInstance.CallRepresentation::class.java, CallRepresentationAdapter())
        .setPrettyPrinting()
        .create()
}
