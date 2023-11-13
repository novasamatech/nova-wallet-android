package io.novafoundation.nova.common.data.network.runtime.binding

import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum

fun <T> bindList(dynamicInstance: Any?, itemBinder: (Any?) -> T): List<T> {
    if (dynamicInstance == null) return emptyList()

    return dynamicInstance.cast<List<*>>().map {
        itemBinder(it)
    }
}

inline fun <T1, T2> bindPair(
    dynamicInstance: Any,
    firstComponent: (Any?) -> T1,
    secondComponent: (Any?) -> T2
): Pair<T1, T2> {
    val (first, second) = dynamicInstance.cast<List<*>>()

    return firstComponent(first) to secondComponent(second)
}

// Maps are encoded as List<Pair<K, V>>
fun <K, V> bindMap(dynamicInstance: Any?, keyBinder: (Any?) -> K, valueBinder: (Any?) -> V): Map<K, V> {
    if (dynamicInstance == null) return emptyMap()

    return dynamicInstance.cast<List<*>>().associateBy(
        keySelector = {
            val (keyRaw, _) = it.cast<List<*>>()

            keyBinder(keyRaw)
        },
        valueTransform = {
            val (_, valueRaw) = it.cast<List<*>>()

            valueBinder(valueRaw)
        }
    )
}

inline fun <reified T : Enum<T>> bindCollectionEnum(
    dynamicInstance: Any?,
    enumValueFromName: (String) -> T = ::enumValueOf
): T {
    return when (dynamicInstance) {
        is String -> enumValueFromName(dynamicInstance) // collection enum
        is DictEnum.Entry<*> -> enumValueFromName(dynamicInstance.name) // dict enum with empty values
        else -> incompatible()
    }
}
