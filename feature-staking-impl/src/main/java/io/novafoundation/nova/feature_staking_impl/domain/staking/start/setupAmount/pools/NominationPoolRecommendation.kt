package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.recommendation.NominationPoolRecommenderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

class NominationPoolRecommendation(
    private val scope: CoroutineScope,
    private val stakingOption: StakingOption,
    private val nominationPoolRecommenderFactory: NominationPoolRecommenderFactory
) : SingleStakingRecommendation {

    private val recommendator = scope.async {
        nominationPoolRecommenderFactory.create(stakingOption, scope)
    }

    override suspend fun recommendedSelection(stake: Balance): StartMultiStakingSelection? {
        val recommendedPool = recommendator.await().recommendedPool() ?: return null

        return NominationPoolSelection(recommendedPool, stakingOption, stake)
    }
}
