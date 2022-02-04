package io.novafoundation.nova.common.utils

interface Filter<T> {
    fun shouldInclude(model: T): Boolean
}

interface NamedFilter<T> : Filter<T> {

    val name: String
}

fun <T> List<T>.applyFilters(filters: List<Filter<T>>): List<T> {
    return filter { item -> filters.all { filter -> filter.shouldInclude(item) } }
}
