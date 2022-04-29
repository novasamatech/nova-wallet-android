package io.novafoundation.nova.common.data.network.runtime.binding

fun <T> bindList(dynamicInstance: Any?, itemBinder: (Any?) -> T): List<T> {
    return dynamicInstance.cast<List<*>>().map {
        itemBinder(it)
    }
}

inline fun <reified T: Enum<T>> bindCollectionEnum(dynamicInstance: Any?): T {
    val enumValue = dynamicInstance.cast<String>()

    return enumValueOf(enumValue)
}
