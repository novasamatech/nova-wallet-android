package io.novafoundation.nova.common.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.math.BigInteger

fun Any?.asGsonParsedNumber(): BigInteger? = when (this) {
    // gson parses integers as double when type is not specified
    is Double -> toLong().toBigInteger()
    is Long -> toBigInteger()
    is Int -> toBigInteger()
    is String -> toBigIntegerOrNull()
    else -> null
}

fun Gson.parseArbitraryObject(src: String): Map<String, Any?>? {
    val typeToken = object : TypeToken<Map<String, Any?>>() {}

    return fromJson(src, typeToken.type)
}

inline fun <reified T> Gson.fromParsedHierarchy(src: Any?): T = fromJson(toJsonTree(src), T::class.java)

inline fun <reified T> Gson.fromJson(src: String): T = fromJson(src, object : TypeToken<T>() {}.type)

inline fun <reified T> Gson.fromJsonOrNull(src: String): T? = runCatching<T> { fromJson(src) }.getOrNull()
