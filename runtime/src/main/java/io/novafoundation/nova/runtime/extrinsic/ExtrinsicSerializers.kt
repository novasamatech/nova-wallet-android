package io.novafoundation.nova.runtime.extrinsic

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import io.novafoundation.nova.common.utils.ByteArrayAsReadableStringSerializer
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

object ExtrinsicSerializers {

    fun gson() = GsonBuilder()
        .registerTypeHierarchyAdapter(ByteArray::class.java, ByteArrayAsReadableStringSerializer())
        .registerTypeHierarchyAdapter(GenericCall.Instance::class.java, GenericCallAdapter())
        .setPrettyPrinting()
        .create()
}
