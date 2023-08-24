package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.direct.DirectStakingTypeDetailsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct.DirectStakingRecommendation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model.EditableStakingType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DirectStakingTypeDetailsProvider(
    directStakingTypeDetailsInteractor: DirectStakingTypeDetailsInteractor,
    override val stakingType: Chain.Asset.StakingType,
    override val recommendationProvider: DirectStakingRecommendation
) : StakingTypeDetailsProvider {

    //private val availabilityValidation = ValidationSystem<> {  }

    override val stakingTypeDetails: Flow<EditableStakingType> = directStakingTypeDetailsInteractor.observeData()
        .map {
            EditableStakingType(
                isAvailable = false,
                stakingTypeDetails = it,
                stakingType = stakingType
            )
        }
}
