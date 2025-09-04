package io.novafoundation.nova.common.data.network.subquery

import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.or

interface SubQueryFilters {

    companion object : SubQueryFilters

    infix fun String.equalTo(value: String) = "$this: { equalTo: \"$value\" }"

    infix fun String.equalToEnum(value: String) = "$this: { equalTo: $value }"

    fun queryParams(
        filter: String
    ): String {
        if (filter.isEmpty()) {
            return ""
        }

        return "(filter: { $filter })"
    }

    fun String.containsFilter(field: String, value: String?): String {
        return if (value != null) {
            "$this: { contains: { $field: \"$value\" } }"
        } else {
            or(
                "$this: { contains: { $field: null } }",
                "not: { $this: { containsKey: \"$field\"} }"
            )
        }
    }
}
