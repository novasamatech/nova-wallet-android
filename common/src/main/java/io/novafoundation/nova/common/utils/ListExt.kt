package io.novafoundation.nova.common.utils

fun <T> List<T>.isSubsetOf(list: List<T>): Boolean {
    return list.containsAll(this)
}

fun <T> Collection<T>.isAllEquals(value: (T) -> Any): Boolean {
    if (isEmpty()) return false

    val first = value(first())
    return all { value(it) == first }
}
