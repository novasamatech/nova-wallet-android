package io.novafoundation.nova.feature_wallet_impl.data.network.model.request

import android.annotation.SuppressLint
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.allFiltersIncluded
import io.novafoundation.nova.feature_wallet_impl.data.network.model.request.SubqueryExpressions.and
import io.novafoundation.nova.feature_wallet_impl.data.network.model.request.SubqueryExpressions.anyOf
import io.novafoundation.nova.feature_wallet_impl.data.network.model.request.SubqueryExpressions.not
import io.novafoundation.nova.feature_wallet_impl.data.network.model.request.SubqueryExpressions.or

private class ModuleRestriction(
    val moduleName: String,
    val restrictedCalls: List<String>
)

private val EXTRINSIC_RESTRICTIONS = listOf(
    ModuleRestriction(
        moduleName = "balances",
        restrictedCalls = listOf(
            "transfer",
            "transferKeepAlive",
            "forceTransfer"
        )
    )
)

class SubqueryHistoryRequest(
    accountAddress: String,
    pageSize: Int = 1,
    cursor: String? = null,
    filters: Set<TransactionFilter>
) {
    val query = """
    {
        query {
            historyElements(
                after: ${if (cursor == null) null else "\"$cursor\""},
                first: $pageSize,
                orderBy: TIMESTAMP_DESC,
                filter: { 
                    address:{ equalTo: "$accountAddress"},
                    ${filters.toQueryFilter()}
                }
            ) {
                pageInfo {
                    startCursor,
                    endCursor
                },
                nodes {
                    id
                    timestamp
                    extrinsicHash
                    address
                    reward
                    extrinsic
                    transfer
                }
            }
        }
    }

    """.trimIndent()

    /*
        or: [ {transfer: { notEqualTo: "null"} },  {extrinsic: { notEqualTo: "null"} } ]
     */
    private fun Set<TransactionFilter>.toQueryFilter(): String {
        val additionalFilters = not(isIgnoredExtrinsic())

        // optimize query in case all filters are on
        return if (allFiltersIncluded()) {
            additionalFilters
        } else {
            val filtersExpressions = map { hasType(it.filterName) }
            val userFilters = anyOf(filtersExpressions)

            userFilters and additionalFilters
        }
    }

    private fun isIgnoredExtrinsic(): String {
        val exists = hasType(TransactionFilter.EXTRINSIC.filterName)

        val restrictedModulesList = EXTRINSIC_RESTRICTIONS.map {
            val restrictedCallsExpressions = it.restrictedCalls.map(::callNamed)

            and(
                moduleNamed(it.moduleName),
                anyOf(restrictedCallsExpressions)
            )
        }

        val hasRestrictedModules = or(restrictedModulesList)

        return and(
            exists,
            hasRestrictedModules
        )
    }

    private fun callNamed(callName: String) = "extrinsic: {contains: {call: \"$callName\"}}"
    private fun moduleNamed(moduleName: String) = "extrinsic: {contains: {module: \"$moduleName\"}}"
    private fun hasType(typeName: String) = "$typeName: {isNull: false}"

    private val TransactionFilter.filterName
        @SuppressLint("DefaultLocale")
        get() = name.toLowerCase()
}
