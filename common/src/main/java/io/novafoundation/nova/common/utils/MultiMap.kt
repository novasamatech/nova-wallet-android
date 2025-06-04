package io.novafoundation.nova.common.utils

typealias MutableMultiMap<K, V> = MutableMap<K, MutableSet<V>>
typealias MutableMultiMapList<K, V> = MutableMap<K, MutableList<V>>
typealias MultiMap<K, V> = Map<K, Set<V>>
typealias MultiMapList<K, V> = Map<K, List<V>>

fun <K, V> mutableMultiMapOf(): MutableMultiMap<K, V> = mutableMapOf()

fun <K, V> mutableMultiListMapOf(): MutableMultiMapList<K, V> = mutableMapOf()

fun <K, V> MutableMultiMap<K, V>.put(key: K, value: V) {
    getOrPut(key, ::mutableSetOf).add(value)
}

@JvmName("putIntoList")
fun <K, V> MutableMultiMapList<K, V>.put(key: K, value: V) {
    getOrPut(key, ::mutableListOf).add(value)
}

fun <K, V> MutableMultiMapList<K, V>.put(key: K, values: List<V>) {
    getOrPut(key, ::mutableListOf).addAll(values)
}
