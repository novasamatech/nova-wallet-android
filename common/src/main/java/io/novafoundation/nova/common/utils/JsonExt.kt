package io.novafoundation.nova.common.utils

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import java.lang.reflect.Type
import java.math.BigInteger

class ByteArrayHexAdapter : JsonSerializer<ByteArray>, JsonDeserializer<ByteArray> {

    override fun serialize(src: ByteArray, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toHexString(withPrefix = true))
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ByteArray {
        return json.asString.fromHex()
    }
}

fun Any?.asGsonParsedNumberOrNull(): BigInteger? = when (this) {
    // gson parses integers as double when type is not specified
    is Double -> toLong().toBigInteger()
    is Long -> toBigInteger()
    is Int -> toBigInteger()
    is String -> toBigIntegerOrNull()
    else -> null
}

fun Any?.asGsonParsedLongOrNull(): Long? = when (this) {
    is Number -> toLong()
    is String -> toLongOrNull()
    else -> null
}

fun Any?.asGsonParsedIntOrNull(): Int? = when (this) {
    is Number -> toInt()
    is String -> toIntOrNull()
    else -> null
}

fun Any?.asGsonParsedNumber(): BigInteger = asGsonParsedNumberOrNull()
    ?: throw IllegalArgumentException("Failed to convert gson-parsed object to number")

fun Gson.parseArbitraryObject(src: String): Map<String, Any?>? {
    val typeToken = object : TypeToken<Map<String, Any?>>() {}

    return fromJson(src, typeToken.type)
}

fun <T> Gson.fromParsedHierarchy(src: Any?, clazz: Class<T>): T = fromJson(toJsonTree(src), clazz)
inline fun <reified T> Gson.fromParsedHierarchy(src: Any?): T = fromParsedHierarchy(src, T::class.java)

inline fun <reified T> Gson.fromJson(src: String): T = fromJson(src, object : TypeToken<T>() {}.type)

inline fun <reified T> Gson.fromJsonOrNull(src: String): T? = runCatching<T> { fromJson(src) }.getOrNull()
