package io.novafoundation.nova.feature_staking_impl.data.repository.datasource.reward

import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType

interface StakingRewardsDataSourceRegistry {

    fun getDataSourceFor(stakingOptionId: StakingOptionId): StakingRewardsDataSource
}

class RealStakingRewardsDataSourceRegistry(
    private val directStakingRewardsDataSource: StakingRewardsDataSource,
    private val poolStakingRewardsDataSource: StakingRewardsDataSource
) : StakingRewardsDataSourceRegistry {

    override fun getDataSourceFor(stakingOptionId: StakingOptionId): StakingRewardsDataSource {
        return when (stakingOptionId.stakingType) {
            StakingType.NOMINATION_POOLS -> poolStakingRewardsDataSource
            else -> directStakingRewardsDataSource
        }
    }
}
