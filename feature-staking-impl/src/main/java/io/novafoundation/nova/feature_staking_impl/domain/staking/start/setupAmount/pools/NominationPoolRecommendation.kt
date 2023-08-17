package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation

class NominationPoolRecommendation : SingleStakingRecommendation {

    override suspend fun recommendedSelection(): StartMultiStakingSelection {
       return NominationPoolSelection()
    }
}
