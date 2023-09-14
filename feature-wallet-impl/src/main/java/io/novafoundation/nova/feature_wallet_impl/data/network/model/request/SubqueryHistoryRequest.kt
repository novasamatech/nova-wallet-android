package io.novafoundation.nova.feature_wallet_impl.data.network.model.request

import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.and
import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.anyOf
import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.not
import io.novafoundation.nova.common.utils.nullIfEmpty
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset

private class ModuleRestriction(
    val moduleName: String,
    val restrictedCalls: List<String>
) {
    companion object
}

private fun ModuleRestriction.Companion.ignoreTransferExtrinsics(
    moduleNames: List<String>
) = moduleNames.map {
    ModuleRestriction(
        moduleName = it,
        restrictedCalls = listOf(
            "transfer",
            "transferKeepAlive",
            "transferAllowDeath",
            "forceTransfer",
            "transferAll"
        )
    )
}

class SubqueryHistoryRequest(
    accountAddress: String,
    pageSize: Int = 1,
    cursor: String? = null,
    filters: Set<TransactionFilter>,
    asset: Asset
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
                    ${filters.toQueryFilter(asset)}
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
                    ${rewardsResponseSections(asset)}
                    extrinsic
                    ${transferResponseSection(asset.type)}
                }
            }
        }
    }

    """.trimIndent()

    /*
        or: [ {transfer: { notEqualTo: "null"} },  {extrinsic: { notEqualTo: "null"} } ]
     */
    private fun Set<TransactionFilter>.toQueryFilter(asset: Asset): String {
        val additionalFilters = not(isIgnoredExtrinsic(asset.type))

        val filtersExpressions = mapNotNull { it.filterExpression(asset) }
        val userFilters = anyOf(filtersExpressions)

        return userFilters and additionalFilters
    }

    private fun TransactionFilter.filterExpression(asset: Asset): String? {
        return when (this) {
            TransactionFilter.TRANSFER -> transfersFilter(asset.type)
            TransactionFilter.REWARD -> rewardsFilter(asset)
            TransactionFilter.EXTRINSIC -> hasExtrinsic()
        }.nullIfEmpty()
    }

    private fun transferResponseSection(assetType: Asset.Type): String {
        return when (assetType) {
            Asset.Type.Native -> "transfer"
            else -> "assetTransfer"
        }
    }

    private fun rewardsResponseSections(asset: Asset): String {
        return rewardsSections(asset).joinToString(separator = "\n")
    }

    private fun rewardsSections(asset: Asset): List<String> {
        return asset.staking.mapNotNull { it.rewardSection() }
    }

    private fun Asset.StakingType.rewardSection(): String? {
        return when (group()) {
            StakingTypeGroup.RELAYCHAIN, StakingTypeGroup.PARACHAIN -> "reward"
            StakingTypeGroup.NOMINATION_POOL -> "poolReward"
            StakingTypeGroup.UNSUPPORTED -> null
        }
    }

    private fun rewardsFilter(asset: Asset): String {
        return anyOf(rewardsSections(asset).map { hasType(it) })
    }

    private fun transfersFilter(assetType: Asset.Type): String {
        return when (assetType) {
            Asset.Type.Native -> hasType("transfer")
            is Asset.Type.Orml -> transferAssetHasId(assetType.currencyIdScale)
            is Asset.Type.Statemine -> transferAssetHasId(assetType.id.toString())
            is Asset.Type.Equilibrium -> transferAssetHasId(assetType.id.toString())
            else -> throw IllegalArgumentException("Unsupported asset")
        }
    }

    private fun hasExtrinsic() = hasType("extrinsic")

    private fun Asset.Type.transferModules(): List<String> {
        return listOf("balances")
    }

    private fun isIgnoredExtrinsic(assetType: Asset.Type): String {
        val exists = hasExtrinsic()
        val transferModules = assetType.transferModules()

        val restrictedModulesList = ModuleRestriction.ignoreTransferExtrinsics(transferModules).map {
            val restrictedCallsExpressions = it.restrictedCalls.map(::callNamed)

            and(
                moduleNamed(it.moduleName),
                anyOf(restrictedCallsExpressions)
            )
        }

        val hasRestrictedModules = anyOf(restrictedModulesList)

        return and(
            exists,
            hasRestrictedModules
        )
    }

    private fun callNamed(callName: String) = "extrinsic: {contains: {call: \"$callName\"}}"
    private fun moduleNamed(moduleName: String) = "extrinsic: {contains: {module: \"$moduleName\"}}"
    private fun hasType(typeName: String) = "$typeName: {isNull: false}"

    private fun transferAssetHasId(assetId: String): String {
        return "assetTransfer: { contains: { assetId: \"$assetId\" } }"
    }
}
