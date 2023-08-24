package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.pool

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.pools.NominationPoolStakingTypeDetailsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools.NominationPoolRecommendation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model.EditableStakingType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PoolStakingTypeDetailsProvider(
    nominationPoolStakingTypeDetailsInteractor: NominationPoolStakingTypeDetailsInteractor,
    override val stakingType: Chain.Asset.StakingType,
    override val recommendationProvider: NominationPoolRecommendation
) : StakingTypeDetailsProvider {

    override val stakingTypeDetails: Flow<EditableStakingType> = nominationPoolStakingTypeDetailsInteractor.observeData()
        .map {
            EditableStakingType(
                isAvailable = false,
                stakingTypeDetails = it,
                stakingType = stakingType
            )
        }
}
