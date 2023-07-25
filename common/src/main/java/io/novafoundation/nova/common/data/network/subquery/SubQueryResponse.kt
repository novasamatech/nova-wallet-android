package io.novafoundation.nova.common.data.network.subquery

class SubQueryResponse<T>(
    val data: T
)

class SubQueryNodes<T>(val nodes: List<T>)

class SubQueryTotalCount(val totalCount: Int)

class SubQueryGroupedAggregates<T : GroupedAggregate>(val groupedAggregates: List<T>)

sealed class GroupedAggregate(val keys: List<String>) {

    class Sum<T>(val sum: T, keys: List<String>) : GroupedAggregate(keys)
}

fun <T> SubQueryGroupedAggregates<GroupedAggregate.Sum<T>>.firstSum(): T? {
    return groupedAggregates.firstOrNull()?.sum
}
