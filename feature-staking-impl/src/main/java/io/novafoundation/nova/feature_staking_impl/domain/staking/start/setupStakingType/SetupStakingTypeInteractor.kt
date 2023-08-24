package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType

import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model.EditableStakingType
import kotlinx.coroutines.flow.Flow

interface SetupStakingTypeInteractor {

    fun getEditableStakingTypes(): Flow<List<EditableStakingType>>

}

class RealSetupStakingTypeInteractor(
    private val stakingTypeDetailsProviders: List<StakingTypeDetailsProvider>
) : SetupStakingTypeInteractor {

    override fun getEditableStakingTypes(): Flow<List<EditableStakingType>> {
        return stakingTypeDetailsProviders.map { it.stakingTypeDetails }
            .combine()
    }
}
