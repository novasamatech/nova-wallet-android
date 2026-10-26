package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.select

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter

class BittensorSelectUnstakePositionViewModel(
    request: BittensorSelectUnstakePositionCommunicator.Request,
    private val communicator: BittensorSelectUnstakePositionCommunicator,
    private val router: StakingRouter
) : BaseViewModel() {

    val options = request.options

    fun select(option: BittensorSelectUnstakePositionCommunicator.Option) {
        if (option !in options) return
        communicator.respond(BittensorSelectUnstakePositionCommunicator.Response(option.position))
        router.back()
    }

    fun back() = router.back()
}
