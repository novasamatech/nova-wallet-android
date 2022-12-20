package io.novafoundation.nova.common.utils

fun <T> List<T>.isSubsetOf(list: List<T>): Boolean {
    return list.containsAll(this)
}
