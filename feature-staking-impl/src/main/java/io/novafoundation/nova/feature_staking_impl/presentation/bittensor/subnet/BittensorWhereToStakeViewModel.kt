package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount.BittensorRootStakePresentation
import io.novafoundation.nova.analytics.BittensorScreen
import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingAnalyticsSelectionMethod
import io.novafoundation.nova.analytics.AnalyticsService

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorQuickPickOrder
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class BittensorWhereToStakeViewModel(
    private val analytics: AnalyticsService,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val router: StakingRouter,
    private val retryController: BittensorRetryController,
    private val validatorCommunicator: BittensorSelectValidatorCommunicator
) : BaseViewModel() {

    private var selectionState = BittensorSelectionState()

    val retryEvents = retryController.events

    val nativeSymbol = retryController.observe(
        step = "native asset",
        transform = { it.catch { cause -> if (cause is CancellationException) throw cause } },
        source = bittensorSubnetInteractor::observeAsset
    )
        .map { it.token.configuration.symbol.value }
        .shareInBackground()

    init {
        validatorCommunicator.responseFlow
            .onEach { selectionState = selectionState.withResponse(it) }
            .launchIn(this)
    }

    fun quickPick(order: BittensorQuickPickOrder) {
        val browseState = BittensorSubnetBrowseState.fromQuickPick(order)
        val visitState = selectionState.copy(browseState = browseState)
        router.openBittensorSubnetAmount(BittensorSubnetAmountPayload(quickPickOrder = order, selectionState = visitState))
    }

    fun rootStaking() {
        analytics.track(
            BittensorAnalyticsEvent.TypeSelected(
                BittensorAnalyticsEvent.NETWORK,
                StakingAnalyticsTarget.ROOT,
                StakingAnalyticsSelectionMethod.QUICK_PICK_ROOT,
                0
            )
        )
        router.openBittensorRootStaking(selectionState = selectionState, presentation = BittensorRootStakePresentation.QUICK_PICK)
    }

    fun back() {
        analytics.track(BittensorAnalyticsEvent.Abandoned(BittensorScreen.WHERE_TO_STAKE, null))
        router.back()
    }
}
