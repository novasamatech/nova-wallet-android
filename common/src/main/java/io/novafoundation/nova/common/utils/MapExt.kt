package io.novafoundation.nova.common.utils

@Suppress("UNCHECKED_CAST")
inline fun <K, reified R> Map<K, *>.filterValuesIsInstance(): Map<K, R> {
    return filterValues { value -> value is R } as Map<K, R>
}

fun <K, V> mapOfNotNullValues(vararg pairs: Pair<K, V?>): Map<K, V> {
    return mapOf(*pairs).filterNotNull()
}

inline fun <K, V> Map<K, List<V>>.filterValueList(action: (V) -> Boolean): Map<K, List<V>> {
    return mapValues { (_, list) -> list.filter { action(it) } }
}
