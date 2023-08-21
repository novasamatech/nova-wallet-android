package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class NominationPoolRecommendation(
    private val stakingOption: StakingOption,
) : SingleStakingRecommendation {

    override suspend fun recommendedSelection(): StartMultiStakingSelection {
        // TODO nomination pool recommendation
        return NominationPoolSelection(
            pool = NominationPool(
                id = PoolId(54),
                stashAccountId = AccountId(32),
                membersCount = 0,
                metadata = null,
                icon = null,
                status = NominationPool.Status.Inactive
            ),
            stakingOption = stakingOption
        )
    }
}
