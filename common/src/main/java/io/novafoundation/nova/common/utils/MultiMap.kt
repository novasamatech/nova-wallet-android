package io.novafoundation.nova.common.utils

typealias MutableMultiMap<K, V> = MutableMap<K, MutableSet<V>>
typealias MultiMap<K, V> = MutableMap<K, out Set<V>>

fun <K, V> mutableMultiMapOf(): MutableMultiMap<K, V> = mutableMapOf()

fun <K, V> MutableMultiMap<K, V>.put(key: K, value: V) {
    getOrPut(key, ::mutableSetOf).add(value)
}
