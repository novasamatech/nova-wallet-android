package io.novafoundation.nova.feature_wallet_impl.data.network.model.request

import io.novafoundation.nova.common.data.network.subquery.SubQueryFilters
import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.and
import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.anyOf
import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.not
import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.or
import io.novafoundation.nova.common.utils.nullIfEmpty
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_impl.data.network.model.subQueryAssetId
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.ext.isSwapSupported
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset

private class ModuleRestriction(
    val moduleName: String,
    val restrictedCalls: List<String>
) {
    companion object
}

private fun ModuleRestriction.Companion.ignoreSpecialOperationTypesExtrinsics() = listOf(
    ModuleRestriction(
        moduleName = "balances",
        restrictedCalls = listOf(
            "transfer",
            "transferKeepAlive",
            "transferAllowDeath",
            "forceTransfer",
            "transferAll"
        )
    ),
    ModuleRestriction(
        moduleName = "assetConversion",
        restrictedCalls = listOf(
            "swapExactTokensForTokens",
            "swapTokensForExactTokens",
        )
    )
)

class SubqueryHistoryRequest(
    accountAddress: String,
    pageSize: Int = 1,
    cursor: String? = null,
    filters: Set<TransactionFilter>,
    asset: Asset,
    chain: Chain,
) : SubQueryFilters {
    val query = """
    {
        query {
            historyElements(
                after: ${if (cursor == null) null else "\"$cursor\""},
                first: $pageSize,
                orderBy: TIMESTAMP_DESC,
                filter: { 
                    address:{ equalTo: "$accountAddress"},
                    ${filters.toQueryFilter(asset, chain)}
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
                    blockNumber
                    address
                    ${rewardsResponseSections(asset)}
                    extrinsic
                    ${transferResponseSection(asset.type)}
                    ${swapResponseSection(chain)}
                }
            }
        }
    }

    """.trimIndent()

    private fun Set<TransactionFilter>.toQueryFilter(asset: Asset, chain: Chain): String {
        val additionalFilters = not(isIgnoredExtrinsic())

        val filtersExpressions = mapNotNull { it.filterExpression(asset, chain) }
        val userFilters = anyOf(filtersExpressions)

        return userFilters and additionalFilters
    }

    private fun TransactionFilter.filterExpression(asset: Asset, chain: Chain): String? {
        return when (this) {
            TransactionFilter.TRANSFER -> transfersFilter(asset.type)
            TransactionFilter.REWARD -> rewardsFilter(asset)
            TransactionFilter.EXTRINSIC -> hasExtrinsic()
            TransactionFilter.SWAP -> swapFilter(chain, asset)
        }.nullIfEmpty()
    }

    private fun transferResponseSection(assetType: Asset.Type): String {
        return when (assetType) {
            Asset.Type.Native -> "transfer"
            else -> "assetTransfer"
        }
    }

    private fun swapResponseSection(chain: Chain): String {
        return if (chain.isSwapSupported()) {
            "swap"
        } else {
            ""
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
            StakingTypeGroup.MYTHOS -> TODO()
            StakingTypeGroup.UNSUPPORTED -> null
        }
    }

    private fun rewardsFilter(asset: Asset): String {
        return anyOf(rewardsSections(asset).map { hasType(it) })
    }

    private fun transfersFilter(assetType: Asset.Type): String {
        return if (assetType == Asset.Type.Native) {
            hasType("transfer")
        } else {
            transferAssetHasId(assetType.subQueryAssetId())
        }
    }

    private fun swapFilter(chain: Chain, asset: Asset): String? {
        if (!chain.isSwapSupported()) return null

        val subQueryAssetId = asset.type.subQueryAssetId()
        return or(
            "swap".containsFilter("assetIdIn", subQueryAssetId),
            "swap".containsFilter("assetIdOut", subQueryAssetId)
        )
    }

    private fun hasExtrinsic() = hasType("extrinsic")

    private fun isIgnoredExtrinsic(): String {
        val exists = hasExtrinsic()

        val restrictedModulesList = ModuleRestriction.ignoreSpecialOperationTypesExtrinsics().map {
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

    private fun transferAssetHasId(assetId: String?): String {
        return "assetTransfer".containsFilter("assetId", assetId)
    }
}
