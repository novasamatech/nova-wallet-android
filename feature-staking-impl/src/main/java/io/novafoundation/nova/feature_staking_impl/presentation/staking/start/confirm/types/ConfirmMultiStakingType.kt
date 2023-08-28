package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.types

import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.model.ConfirmMultiStakingTypeModel
import kotlinx.coroutines.flow.Flow

interface ConfirmMultiStakingType {

    val stakingTypeModel: Flow<ConfirmMultiStakingTypeModel>

    suspend fun onStakingTypeDetailsClicked()
}
