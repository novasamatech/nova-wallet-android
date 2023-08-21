package io.novafoundation.nova.feature_staking_impl.domain.staking.setupStakingType

import io.novafoundation.nova.feature_staking_impl.domain.staking.setupStakingType.model.StakingTypeEditingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface SetupStakingTypeInteractor {

    fun getEditableStakingTypes(): Flow<StakingTypeEditingState>

    fun selectDirectStaking()

    fun selectPoolStaking()

    fun apply()

    fun setValidators()

    fun setPool()
}

class RealSetupStakingTypeInteractor : SetupStakingTypeInteractor {

    override fun getEditableStakingTypes(): Flow<StakingTypeEditingState> {
        return emptyFlow()
    }

    override fun selectDirectStaking() {

    }

    override fun selectPoolStaking() {

    }

    override fun apply() {

    }

    override fun setValidators() {

    }

    override fun setPool() {

    }
}
