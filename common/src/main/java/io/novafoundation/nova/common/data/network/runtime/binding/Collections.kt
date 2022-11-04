package io.novafoundation.nova.common.data.network.runtime.binding

import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum

fun <T> bindList(dynamicInstance: Any?, itemBinder: (Any?) -> T): List<T> {
    if (dynamicInstance == null) return emptyList()

    return dynamicInstance.cast<List<*>>().map {
        itemBinder(it)
    }
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
