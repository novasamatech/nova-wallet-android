package io.novafoundation.nova.common.utils.search

fun <T> Iterable<T>.filterWith(searchFilter: SearchFilter<T>): List<T> {
    return filter { searchFilter.filter(it) }
}
