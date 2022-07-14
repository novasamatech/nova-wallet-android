package io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.LoadingState
import kotlinx.coroutines.flow.Flow

abstract class CurrentStakeTargetsViewModel : BaseViewModel() {

    abstract val currentStakeTargetsFlow: Flow<LoadingState<List<Any>>>

    abstract val warningFlow: Flow<String?>

    abstract val titleFlow: Flow<String>

    abstract fun stakeTargetInfoClicked(address: String)

    abstract fun backClicked()

    abstract fun changeClicked()
}
