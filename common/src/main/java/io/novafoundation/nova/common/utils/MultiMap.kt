package io.novafoundation.nova.common.utils

typealias MutableMultiMap<K, V> = MutableMap<K, MutableSet<V>>
typealias MutableMultiMapList<K, V> = MutableMap<K, MutableList<V>>
typealias MultiMap<K, V> = Map<K, Set<V>>
typealias MultiMapList<K, V> = Map<K, List<V>>

fun <K, V> mutableMultiMapOf(): MutableMultiMap<K, V> = mutableMapOf()

fun <K, V> mutableMultiListMapOf(): MutableMultiMapList<K, V> = mutableMapOf()

inline fun <K, V> buildMultiMap(builder: MutableMultiMap<K, V>.() -> Unit): MultiMap<K, V> = mutableMultiMapOf<K, V>()
    .apply(builder)

fun <K, V> MutableMultiMap<K, V>.put(key: K, value: V) {
    getOrPut(key, ::mutableSetOf).add(value)
}

fun <K, V> MutableMultiMap<K, V>.putAll(key: K, values: Collection<V>) {
    getOrPut(key, ::mutableSetOf).addAll(values)
}

fun <K, V> MutableMultiMap<K, V>.putAll(other: MultiMap<K, V>) {
    other.forEach { (k, v) ->
        putAll(k, v)
    }
}

@JvmName("putIntoList")
fun <K, V> MutableMultiMapList<K, V>.put(key: K, value: V) {
    getOrPut(key, ::mutableListOf).add(value)
}

fun <K, V> MutableMultiMapList<K, V>.put(key: K, values: List<V>) {
    getOrPut(key, ::mutableListOf).addAll(values)
}

inline fun <K, V> buildMultiMapList(builder: MutableMultiMapList<K, V>.() -> Unit): MultiMapList<K, V> {
    return mutableMultiListMapOf<K, V>().apply(builder)
}
