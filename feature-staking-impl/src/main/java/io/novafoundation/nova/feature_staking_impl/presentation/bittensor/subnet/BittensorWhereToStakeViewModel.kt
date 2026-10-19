package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorQuickPickOrder
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class BittensorWhereToStakeViewModel(
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val router: StakingRouter,
    private val retryController: BittensorRetryController
) : BaseViewModel() {

    val retryEvents = retryController.events

    val nativeSymbol = retryController.observe(
        step = "native asset",
        transform = { it.catch { cause -> if (cause is CancellationException) throw cause } },
        source = bittensorSubnetInteractor::observeAsset
    )
        .map { it.token.configuration.symbol.value }
        .shareInBackground()

    fun quickPick(order: BittensorQuickPickOrder) {
        router.openBittensorSubnetAmount(BittensorSubnetAmountPayload(quickPickOrder = order))
    }

    fun rootStaking() {
        router.openBittensorRootStaking()
    }

    fun back() {
        router.back()
    }
}
