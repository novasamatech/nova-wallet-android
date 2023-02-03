package io.novafoundation.nova.common.utils

inline fun <reified K, reified R> Map<*, *>.filterIsInstance(): Map<K, R> {
    val newMap = mutableMapOf<K, R>()
    forEach {
        val key = it.key
        val value = it.value
        if (key is K && value is R) {
            newMap[key] = value
        }
    }
    return newMap
}
