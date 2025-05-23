package io.novafoundation.nova.common.utils

typealias MutableMultiMap<K, V> = MutableMap<K, MutableSet<V>>
typealias MultiMap<K, V> = Map<K, Set<V>>
typealias MultiMapList<K, V> = Map<K, List<V>>

fun <K, V> mutableMultiMapOf(): MutableMultiMap<K, V> = mutableMapOf()

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
