package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats

import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import io.novafoundation.nova.common.utils.asPerbill
import io.novafoundation.nova.common.utils.atLeastZero
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
            val request = StakingStatsRequest(stakingAccounts, stakingChains)
            val dashboardApiUrl = stakingGlobalConfigRepository.getStakingGlobalConfig().multiStakingApiUrl
            val response = api.fetchStakingStats(request, dashboardApiUrl).data

            val earnings = response.stakingApies.associatedById()
            val rewards = response.rewards?.associatedById() ?: emptyMap()
            val slashes = response.slashes?.associatedById() ?: emptyMap()
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
}
