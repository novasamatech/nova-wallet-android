package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats

import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import io.novafoundation.nova.common.utils.asPerbill
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.common.utils.toPercent
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api.StakingStatsApi
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api.StakingStatsRequest
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api.StakingStatsResponse
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api.StakingStatsResponse.AccumulatedReward
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api.StakingStatsResponse.WithStakingId
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api.StakingStatsRewards
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api.mapSubQueryIdToStakingType
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingGlobalConfigRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.UTILITY_ASSET_ID
import io.novafoundation.nova.runtime.ext.timelineChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface StakingStatsDataSource {

    suspend fun fetchStakingStats(stakingAccounts: StakingAccounts, stakingChains: List<Chain>): MultiChainStakingStats
}

class RealStakingStatsDataSource(
    private val api: StakingStatsApi,
    private val stakingGlobalConfigRepository: StakingGlobalConfigRepository
) : StakingStatsDataSource {

    override suspend fun fetchStakingStats(
        stakingAccounts: StakingAccounts,
        stakingChains: List<Chain>
    ): MultiChainStakingStats = withContext(Dispatchers.IO) {
        retryUntilDone {
            val chainsById = stakingChains.associateBy { it.id }
            val request = StakingStatsRequest(stakingAccounts, stakingChains)
            val dashboardApiUrl = stakingGlobalConfigRepository.getStakingGlobalConfig().multiStakingApiUrl
            val response = api.fetchStakingStats(request, dashboardApiUrl).data

            val earnings = response.stakingApies.associatedById()
            val rewards = response.rewards?.associatedById()?.aggregateWithTimelineChain(chainsById) ?: emptyMap()
            val slashes = response.slashes?.associatedById()?.aggregateWithTimelineChain(chainsById) ?: emptyMap()
            val activeStakers = response.activeStakers?.groupedById() ?: emptyMap()

            request.stakingKeysMapping.mapValues { (originalStakingOptionId, stakingKeys) ->
                val totalReward = rewards.getPlanks(originalStakingOptionId) - slashes.getPlanks(originalStakingOptionId)

                val stakingStatusAddress = stakingKeys.stakingStatusAddress
                val stakingOptionActiveStakers = activeStakers[stakingKeys.stakingStatusOptionId].orEmpty()
                val isStakingActive = stakingStatusAddress != null && stakingStatusAddress in stakingOptionActiveStakers

                ChainStakingStats(
                    estimatedEarnings = earnings[originalStakingOptionId]?.maxAPY.orZero().asPerbill().toPercent(),
                    accountPresentInActiveStakers = isStakingActive,
                    rewards = totalReward.atLeastZero()
                )
            }
        }
    }

    private fun Map<StakingOptionId, AccumulatedReward>.getPlanks(key: StakingOptionId): Balance {
        return get(key)?.amount?.toBigInteger().orZero()
    }

    private fun <T : WithStakingId> SubQueryNodes<T>.associatedById(): Map<StakingOptionId, T> {
        return nodes.associateBy {
            StakingOptionId(
                chainId = it.networkId.removeHexPrefix(),
                chainAssetId = UTILITY_ASSET_ID,
                stakingType = mapSubQueryIdToStakingType(it.stakingType)
            )
        }
    }

    private fun SubQueryNodes<StakingStatsResponse.ActiveStaker>.groupedById(): Map<StakingOptionId, List<String>> {
        return nodes.groupBy(
            keySelector = {
                StakingOptionId(
                    chainId = it.networkId.removeHexPrefix(),
                    chainAssetId = UTILITY_ASSET_ID,
                    stakingType = mapSubQueryIdToStakingType(it.stakingType)
                )
            },
            valueTransform = { it.address }
        )
    }

    private fun StakingStatsRewards.associatedById(): Map<StakingOptionId, AccumulatedReward> {
        return groupedAggregates.associateBy(
            keySelector = { rewardAggregate ->
                val (networkId, stakingTypeRaw) = rewardAggregate.keys

                StakingOptionId(
                    chainId = networkId.removeHexPrefix(),
                    chainAssetId = UTILITY_ASSET_ID,
                    stakingType = mapSubQueryIdToStakingType(stakingTypeRaw)
                )
            },
            valueTransform = { rewardAggregate -> rewardAggregate.sum }
        )
    }

    /**
     * Aggregates chain.rewards with timelineChain.rewards.
     * Consider cases when in rewards map we have only timelineChain.rewards. In that case we aggregate rewards to assetHubChain
     * @return Map with aggregated rewards (timeline chains will be excluded from map)
     */
    private fun Map<StakingOptionId, AccumulatedReward>.aggregateWithTimelineChain(
        chains: Map<String, Chain>
    ): Map<StakingOptionId, AccumulatedReward> {
        val result = mutableMapOf<StakingOptionId, AccumulatedReward>()

        val assetHubToTimeline = chains.values.associate { it.id to it.timelineChainId() }.filterNotNull()
        val timelineToAssetHub = assetHubToTimeline.entries.associate { it.value to it.key }

        val visited = mutableSetOf<StakingOptionId>()

        for ((key, reward) in this) {
            if (key in visited) continue

            val pairedChainId = timelineToAssetHub[key.chainId] ?: assetHubToTimeline[key.chainId]
            val pairedKey = pairedChainId?.let { key.copy(chainId = it) }

            val pairedReward = this[pairedKey]?.amount.orZero()
            val aggregatedAmount = reward.amount + pairedReward

            val outputKey = if (timelineToAssetHub.containsKey(key.chainId)) {
                // Use assetHub chain in case if current key is timeline chain
                pairedKey!!
            } else {
                key
            }

            visited += outputKey

            result[outputKey] = AccumulatedReward(aggregatedAmount)
        }

        return result
    }
}
