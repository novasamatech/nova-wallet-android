package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api

import io.novafoundation.nova.common.data.network.subquery.SubQueryFilters
import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.and
import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.anyOf
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.createStakingOption
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.StakingAccounts
import io.novafoundation.nova.feature_staking_impl.data.fullId
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.data.unwrapNominationPools
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.supportedStakingOptions
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.extensions.requireHexPrefix

class StakingStatsRequest(stakingAccounts: StakingAccounts, chains: List<Chain>) {

    @Transient
    val stakingKeysMapping: Map<StakingOptionId, StakingKeys> = constructStakingTypeOverrides(chains, stakingAccounts)

    val query = """
    {
        activeStakers${constructFilters(chains, FilterParent.STAKING_STATUS)} {
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
        
        rewards: rewards${constructFilters(chains, FilterParent.REWARD)} {
            groupedAggregates(groupBy: [NETWORK_ID,  STAKING_TYPE]) {
                sum {
                    amount
                }
      
                keys
            }
        }
        
        slashes: rewards${constructFilters(chains, FilterParent.SLASH)} {
            groupedAggregates(groupBy: [NETWORK_ID,  STAKING_TYPE]) {
                sum {
                    amount
                }
      
                keys
            }
        }
    }
    """.trimIndent()

    private fun constructStakingTypeOverrides(
        chains: List<Chain>,
        stakingAccounts: StakingAccounts
    ): Map<StakingOptionId, StakingKeys> {
        return chains.flatMap { chain ->
            val utilityAsset = chain.utilityAsset

            utilityAsset.supportedStakingOptions().mapNotNull { stakingType ->
                val stakingOption = createStakingOption(chain, utilityAsset, stakingType)
                val stakingOptionId = stakingOption.fullId
                val stakingOptionAccounts = stakingAccounts[stakingOptionId]

                val stakingKeys = StakingKeys(
                    otherStakingOptionId = stakingOptionId,
                    stakingStatusAddress = stakingOptionAccounts?.stakingStatus?.value?.let(chain::addressOf),
                    rewardsAddress = stakingOptionAccounts?.rewards?.value?.let(chain::addressOf),
                    stakingStatusOptionId = stakingOptionId.copy(stakingType = stakingOption.unwrapNominationPools().stakingType)
                )

                stakingOptionId to stakingKeys
            }
        }.toMap()
    }

    private fun constructFilters(chains: List<Chain>, filterParent: FilterParent): String = with(SubQueryFilters) {
        val perChain = chains.mapNotNull { chain ->
            val hasTypeAndAddressOptions = hasTypeAndAddressOptions(chain, filterParent)

            if (hasTypeAndAddressOptions.isEmpty()) return@mapNotNull null

            hasNetwork(chain.id.requireHexPrefix()) and anyOf(hasTypeAndAddressOptions)
        }

        val filters = appendFiltersSpecificToParent(baseFilters = anyOf(perChain), filterParent)

        queryParams(filter = filters)
    }

    private fun SubQueryFilters.Companion.hasTypeAndAddressOptions(
        chain: Chain,
        filterParent: FilterParent,
    ): List<String> {
        val utilityAsset = chain.utilityAsset

        return utilityAsset.supportedStakingOptions().mapNotNull { stakingType ->
            val stakingOption = createStakingOption(chain, utilityAsset, stakingType)
            val stakingOptionId = stakingOption.fullId

            val stakingKeys = stakingKeysMapping[stakingOptionId] ?: return@mapNotNull null
            val address = stakingKeys.addressFor(filterParent) ?: return@mapNotNull null

            val requestStakingType = stakingKeys.stakingTypeFor(filterParent)
            val requestStakingTypeId = mapStakingTypeToSubQueryId(requestStakingType) ?: return@mapNotNull null

            hasAddress(address) and hasStakingType(requestStakingTypeId)
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

    private fun StakingKeys.addressFor(filterParent: FilterParent): String? {
        return when (filterParent) {
            FilterParent.REWARD, FilterParent.SLASH -> rewardsAddress
            FilterParent.STAKING_STATUS -> stakingStatusAddress
        }
    }

    private fun StakingKeys.stakingTypeFor(filterParent: FilterParent): Chain.Asset.StakingType {
        return when (filterParent) {
            FilterParent.REWARD, FilterParent.SLASH -> otherStakingOptionId.stakingType
            FilterParent.STAKING_STATUS -> stakingStatusOptionId.stakingType
        }
    }

    private enum class FilterParent {

        REWARD, SLASH, STAKING_STATUS
    }

    class StakingKeys(
        val otherStakingOptionId: StakingOptionId,
        val stakingStatusOptionId: StakingOptionId,
        val stakingStatusAddress: String?,
        val rewardsAddress: String?,
    )
}
