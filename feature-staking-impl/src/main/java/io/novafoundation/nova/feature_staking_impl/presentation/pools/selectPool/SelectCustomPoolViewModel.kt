package io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.PoolStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorStakeTargetModel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

class SelectCustomPoolViewModel(
    private val router: StakingRouter
) : BaseViewModel() {

    val poolModelsFlow = emptyFlow<List<PoolStakeTargetModel>>()
        .inBackground()
        .share()

    val selectedTitle = emptyFlow<String>()
        .shareInBackground()

    val fillWithRecommendedEnabled = flowOf { false }
        .share()

    fun backClicked() {
        router.back()
    }

    fun poolInfoClicked(poolStakeModel: PoolStakeTargetModel) = launch {

    }

    fun poolClicked(poolStakeModel: PoolStakeTargetModel) {

    }

    fun searchClicked() {

    }

    fun selectRecommended() {

    }
}
