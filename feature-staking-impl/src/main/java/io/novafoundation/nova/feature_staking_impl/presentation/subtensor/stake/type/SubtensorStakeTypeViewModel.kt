package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.type

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SubtensorStakeTypeViewModel(
    private val router: StakingRouter,
) : BaseViewModel() {

    enum class Selection { ROOT, SUBNET }

    private val _selection = MutableStateFlow(Selection.ROOT)
    val selection: StateFlow<Selection> = _selection.asStateFlow()

    fun selectRoot() {
        _selection.value = Selection.ROOT
    }

    fun selectSubnet() {
        _selection.value = Selection.SUBNET
    }

    fun continueClicked() {
        when (_selection.value) {
            Selection.ROOT -> router.openSubtensorStakeSetup(SubtensorStakingConstants.ROOT_NETUID)
            Selection.SUBNET -> router.openSubtensorSubnetPicker()
        }
    }

    fun backClicked() {
        router.back()
    }
}
