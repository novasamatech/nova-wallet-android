package io.novafoundation.nova.runtime.extrinsic

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import java.lang.reflect.Type

private val callSerializerGson = GsonBuilder()
    .registerTypeHierarchyAdapter(ByteArray::class.java, ByteArrayHexAdapter())
    .registerTypeHierarchyAdapter(GenericCall.Instance::class.java, GenericCallAdapter())
    .setPrettyPrinting()
    .create()

private class GenericCallAdapter : JsonSerializer<GenericCall.Instance> {

    override fun serialize(src: GenericCall.Instance, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonObject().apply {
            add("module", JsonPrimitive(src.module.name))
            add("function", JsonPrimitive(src.function.name))
            add("args", context.serialize(src.arguments))
        }
    }
}

private class ByteArrayHexAdapter : JsonSerializer<ByteArray> {

    override fun serialize(src: ByteArray, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toHexString(withPrefix = true))
    }
}

fun GenericCall.Instance.rawData(): String {
    return callSerializerGson.toJson(this)
}
