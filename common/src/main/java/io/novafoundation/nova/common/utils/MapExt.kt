package io.novafoundation.nova.common.utils

@Suppress("UNCHECKED_CAST")
inline fun <K, reified R> Map<K, *>.filterValuesIsInstance(): Map<K, R> {
    return filterValues { value -> value is R } as Map<K, R>
}

fun <K, V> mapOfNotNullValues(vararg pairs: Pair<K, V?>): Map<K, V> {
    return mapOf(*pairs).filterNotNull()
}

/**
 * Groups items Sequentially step by step. Awaits that items is sorted
 * Defines a group of elements and puts all subsequent elements in that group until the next group found
 * @param keyForEmptyValue - is called when there is no group for item: [Item, Item, Group, Item Item] - for this case will create Group for first two items
 */
fun <T> List<T>.groupSequentially(
    isKey: (T) -> Boolean,
    keyForEmptyValue: (T) -> T // Generates key if there is no key for value in list
): Map<T, List<T>> {
    val resultMap = mutableMapOf<T, MutableList<T>>()

    var currentValues: MutableList<T>? = null
    forEach {
        if (isKey(it)) {
            currentValues = mutableListOf()
            resultMap[it] = currentValues!!
        } else {
            if (currentValues == null) {
                currentValues = mutableListOf()
                resultMap[keyForEmptyValue(it)] = currentValues!!
            }
            currentValues!! += it
        }
    }

    return resultMap
}
