package io.novafoundation.nova.common.data.network.subquery

import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.or

interface SubQueryFilters {

    companion object : SubQueryFilters

    infix fun String.equalTo(value: String) = "$this: { equalTo: \"$value\" }"

    infix fun String.equalTo(value: Boolean) = "$this: { equalTo: $value }"

    infix fun String.equalTo(value: Int) = "$this: { equalTo: $value }"

    infix fun String.equalToEnum(value: String) = "$this: { equalTo: $value }"

    fun queryParams(
        filter: String
    ): String {
        if (filter.isEmpty()) {
            return ""
        }

        return "(filter: { $filter })"
    }

    infix fun String.presentIn(values: List<String>): String {
        val queryValues = values.joinToString(separator = ",") { "\"${it}\"" }
        return "$this: { in: [$queryValues] }"
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
