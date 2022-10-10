package io.novafoundation.nova.common.list

typealias GroupedList<K, V> = Map<K, List<V>>

fun <K, V> emptyGroupedList() = emptyMap<K, V>()

fun <K : Any, V : Any> GroupedList<K, V>.toListWithHeaders(): List<Any> = flatMap { (groupKey, values) ->
    listOf(groupKey) + values
}

inline fun <K1, K2 : Any, V1, V2 : Any> GroupedList<K1, V1>.toListWithHeaders(
    keyMapper: (K1, List<V1>) -> K2?,
    valueMapper: (V1) -> V2
) = flatMap { (key, values) ->
    val mappedKey = keyMapper(key, values)
    val mappedValues = values.map(valueMapper)

    if (mappedKey != null) {
        listOf(mappedKey) + mappedValues
    } else {
        mappedValues
    }
}

fun <K, V> GroupedList<K, V>.toValueList(): List<V> = flatMap { (_, values) -> values }
