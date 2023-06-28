package io.novafoundation.nova.common.data.network.subquery

interface SubQueryFilters {

    companion object : SubQueryFilters

    infix fun String.equalTo(value: String) = "$this: { equalTo: \"$value\" }"

    fun queryParams(
        filter: String
    ): String {
        if (filter.isEmpty()) {
            return ""
        }

        return "(filter: { $filter })"
    }
}
