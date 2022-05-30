package io.novafoundation.nova.common.data.network.runtime.binding

import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum

fun <T> bindList(dynamicInstance: Any?, itemBinder: (Any?) -> T): List<T> {
    return dynamicInstance.cast<List<*>>().map {
        itemBinder(it)
    }
}

inline fun <reified T : Enum<T>> bindCollectionEnum(dynamicInstance: Any?): T {
    return when (dynamicInstance) {
        is String -> enumValueOf(dynamicInstance) // collection enum
        is DictEnum.Entry<*> -> enumValueOf(dynamicInstance.name) // dict enum with empty values
        else -> incompatible()
    }
}
