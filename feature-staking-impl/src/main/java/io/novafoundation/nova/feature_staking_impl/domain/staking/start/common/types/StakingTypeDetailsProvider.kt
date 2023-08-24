package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model.EditableStakingType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow


interface StakingTypeDetailsProvider {

    val stakingType: Chain.Asset.StakingType

    val stakingTypeDetails: Flow<EditableStakingType>

    val recommendationProvider: SingleStakingRecommendation
}
