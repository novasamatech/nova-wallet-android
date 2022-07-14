package io.novafoundation.nova.common.list

typealias GroupedList<K, V> = Map<K, List<V>>

fun <K, V> emptyGroupedList() = emptyMap<K, V>()

fun <K: Any, V: Any> GroupedList<K, V>.toListWithHeaders(): List<Any> = flatMap { (groupKey, values) ->
    listOf(groupKey) + values
}

fun <K, V> GroupedList<K, V>.toValueList(): List<V> = flatMap { (_, values) -> values }
