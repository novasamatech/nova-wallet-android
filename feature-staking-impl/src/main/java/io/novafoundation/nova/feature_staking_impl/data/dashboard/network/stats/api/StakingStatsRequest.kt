package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api

import io.novafoundation.nova.common.data.network.subquery.SubQueryFilters
import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.and
import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.anyOf
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.StakingAccounts
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.StakingOptionAccounts
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.supportedStakingOptions
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapStakingTypeToStakingString
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.extensions.requireHexPrefix
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class StakingStatsRequest(stakingAccounts: StakingAccounts, chains: List<Chain>) {

    val query = """
    {
        activeStakers${constructFilters(stakingAccounts, chains, FilterParent.STAKING_STATUS)} {
            nodes {
                networkId
                stakingType
                address
            }
        }
        
        stakingApies {
            nodes {
                networkId
                stakingType
                maxAPY
            }
        }
        
        rewards: rewards${constructFilters(stakingAccounts, chains, FilterParent.REWARD)} {
            groupedAggregates(groupBy: [NETWORK_ID,  STAKING_TYPE]) {
                sum {
                    amount
                }
      
                keys
            }
        }
        
        slashes: rewards${constructFilters(stakingAccounts, chains, FilterParent.SLASH)} {
            groupedAggregates(groupBy: [NETWORK_ID,  STAKING_TYPE]) {
                sum {
                    amount
                }
      
                keys
            }
        }
    }
    """.trimIndent()

    private fun constructFilters(
        stakingAccounts: StakingAccounts,
        chains: List<Chain>,
        filterParent: FilterParent,
    ): String = with(SubQueryFilters) {
        val perChain = chains.mapNotNull { chain ->
            val hasTypeAndAddressOptions = hasTypeAndAddressOptions(chain, stakingAccounts, filterParent)

            if (hasTypeAndAddressOptions.isEmpty()) return@mapNotNull null

            hasNetwork(chain.id.requireHexPrefix()) and anyOf(hasTypeAndAddressOptions)
        }

        val filters = appendFiltersSpecificToParent(baseFilters = anyOf(perChain), filterParent)

        queryParams(filter = filters)
    }

    private fun SubQueryFilters.Companion.hasTypeAndAddressOptions(
        chain: Chain,
        stakingAccounts: StakingAccounts,
        filterParent: FilterParent,
    ): List<String> {
        val utilityAsset = chain.utilityAsset

        return utilityAsset.supportedStakingOptions().mapNotNull { stakingType ->
            val stakingOptionId = StakingOptionId(chain.id, utilityAsset.id, stakingType)
            val stakingOptionAccounts = stakingAccounts[stakingOptionId] ?: return@mapNotNull null
            val stakingTypeString = mapStakingTypeToStakingString(stakingType) ?: return@mapNotNull null

            val accountId = stakingOptionAccounts.accountIdFor(filterParent)

            hasAddress(chain.addressOf(accountId)) and hasStakingType(stakingTypeString)
        }
    }

    private fun SubQueryFilters.hasNetwork(chainId: ChainId): String {
        return "networkId" equalTo chainId
    }

    private fun SubQueryFilters.hasStakingType(stakingType: String): String {
        return "stakingType" equalTo stakingType
    }

    private fun SubQueryFilters.hasAddress(address: String): String {
        return "address" equalTo address
    }

    private fun SubQueryFilters.hasRewardType(type: String): String {
        return "type" equalToEnum type
    }

    private fun SubQueryFilters.Companion.appendFiltersSpecificToParent(baseFilters: String, filterParent: FilterParent): String {
        return when (filterParent) {
            FilterParent.REWARD -> baseFilters and hasRewardType("reward")
            FilterParent.SLASH -> baseFilters and hasRewardType("slash")
            FilterParent.STAKING_STATUS -> baseFilters
        }
    }

    private fun StakingOptionAccounts.accountIdFor(filterParent: FilterParent): AccountId {
        val accountIdKey = when (filterParent) {
            FilterParent.REWARD, FilterParent.SLASH -> rewards
            FilterParent.STAKING_STATUS -> stakingStatus
        }

        return accountIdKey.value
    }

    private enum class FilterParent {

        REWARD, SLASH, STAKING_STATUS
    }
}
