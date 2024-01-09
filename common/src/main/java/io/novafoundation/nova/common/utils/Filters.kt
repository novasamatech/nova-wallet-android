package io.novafoundation.nova.common.utils

import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot

interface Filter<T> {
    fun shouldInclude(model: T): Boolean
}

interface RuntimeDependent {

    fun availableIn(runtime: RuntimeSnapshot): Boolean
}

interface PalletBasedFilter<T> : Filter<T>, RuntimeDependent {

    override fun availableIn(runtime: RuntimeSnapshot) = true
}

interface NamedFilter<T> : Filter<T> {

    val name: String
}

interface OptionsFilter<T, O> : Filter<T> {

    val options: List<O>
}

class EmptyFilter<T> : Filter<T> {

    override fun shouldInclude(model: T) = true
}

fun <T> List<T>.applyFilters(filters: List<Filter<T>>): List<T> {
    return filter { item -> filters.all { filter -> filter.shouldInclude(item) } }
}

fun <T> List<T>.applyFilter(filter: Filter<in T>): List<T> {
    return filter { item -> filter.shouldInclude(item) }
}
