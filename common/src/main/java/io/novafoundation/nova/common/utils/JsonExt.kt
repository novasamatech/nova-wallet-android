package io.novafoundation.nova.common.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.math.BigInteger

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
