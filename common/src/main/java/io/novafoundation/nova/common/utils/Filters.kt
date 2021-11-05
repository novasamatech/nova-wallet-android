package io.novafoundation.nova.common.utils

interface Filter<T> {
    fun shouldInclude(model: T): Boolean
}

fun <T> List<T>.applyFilters(filters: List<Filter<T>>): List<T> {
    return filter { item -> filters.all { filter -> filter.shouldInclude(item) } }
}
