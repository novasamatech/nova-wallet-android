package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.analytics.StakingFlowSource
import io.novafoundation.nova.analytics.StakingResultAction
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorResultAction

/**
 * Leaves a Bittensor confirmation screen once its operation is done or unconfirmed, as iOS
 * `SubtensorOperationResultPresenter.activate(action:)` and `goBack()`. One instance per screen: the first exit wins,
 * so a second tap does not navigate twice.
 */
class BittensorResultNavigation(
    private val router: StakingRouter,
    private val analytics: AnalyticsService,
    private val flow: StakingFlow,
    private val target: StakingAnalyticsTarget,
    private val newPosition: Boolean,
    private val trackActions: Boolean = true
) {

    private var finished = false

    /** Root sheet buttons once the operation is done or unconfirmed, as iOS `landingActions`. */
    fun landingActions(emptiesPosition: Boolean = false): List<BittensorResultAction> = when {
        newPosition -> listOf(BittensorResultAction.VIEW_POSITION)
        emptiesPosition -> listOf(BittensorResultAction.YOUR_BITTENSOR)
        else -> listOf(BittensorResultAction.YOUR_BITTENSOR, BittensorResultAction.BACK_TO_POSITION)
    }

    /** Root sheet buttons once the operation is done: a new position also offers "Discover subnets" (iOS `createDoneSheet`). */
    fun completedActions(emptiesPosition: Boolean = false): List<BittensorResultAction> = if (newPosition) {
        listOf(BittensorResultAction.DISCOVER_SUBNETS, BittensorResultAction.VIEW_POSITION)
    } else {
        landingActions(emptiesPosition)
    }

    /** A result button: "View position" and "Your Bittensor" open the portfolio, "Close" and "Back to position" the flow origin. */
    fun activate(action: BittensorResultAction, emptiesPosition: Boolean = false, retry: () -> Unit) {
        when (action) {
            BittensorResultAction.DONE -> done(emptiesPosition)
            BittensorResultAction.TRY_AGAIN -> if (!finished) {
                track(StakingResultAction.TRY_AGAIN)
                retry()
            }
            BittensorResultAction.CLOSE -> finish(BittensorFlowExit.ORIGIN, StakingResultAction.CLOSE)
            BittensorResultAction.BACK_TO_POSITION -> finish(BittensorFlowExit.ORIGIN, StakingResultAction.BACK_TO_POSITION)
            BittensorResultAction.VIEW_POSITION -> finish(BittensorFlowExit.PORTFOLIO, StakingResultAction.VIEW_POSITION)
            BittensorResultAction.YOUR_BITTENSOR -> finish(BittensorFlowExit.PORTFOLIO, StakingResultAction.YOUR_BITTENSOR)
            BittensorResultAction.DISCOVER_SUBNETS -> discoverSubnets()
        }
    }

    /** "Done", including "Done" on an unconfirmed result. */
    fun done(emptiesPosition: Boolean = false) {
        val exit = bittensorCompletedFlowExit(newPosition, emptiesPosition)
        finish(exit, exit.doneAnalyticsAction)
    }

    /** "View position": the new position on the portfolio, otherwise the position the flow started from. */
    fun viewPosition(emptiesPosition: Boolean = false) {
        val exit = bittensorCompletedFlowExit(newPosition, emptiesPosition)
        val action = when {
            newPosition -> StakingResultAction.VIEW_POSITION
            exit == BittensorFlowExit.PORTFOLIO -> StakingResultAction.YOUR_BITTENSOR
            else -> StakingResultAction.BACK_TO_POSITION
        }
        finish(exit, action)
    }

    /**
     * Only offered on a new position, as iOS `createDoneSheet`; [network] is the chain name for `staking_flow_opened`,
     * which follows the result action as in iOS `activate(action:)`.
     */
    fun discoverSubnets(network: String = BittensorAnalyticsEvent.NETWORK) {
        if (finished) return
        finished = true

        track(StakingResultAction.DISCOVER_SUBNETS)
        if (trackActions) analytics.track(AnalyticsEvent.StakingFlowOpened(network, StakingFlowSource.RESULT_DISCOVER.value))
        router.finishBittensorFlow(BittensorFlowExit.SUBNET_DISCOVERY)
    }

    /** Back after the operation finished: same destination as "Done", not reported as an action. */
    fun leave(emptiesPosition: Boolean = false) = finish(bittensorCompletedFlowExit(newPosition, emptiesPosition), action = null)

    private fun finish(exit: BittensorFlowExit, action: StakingResultAction?) {
        if (finished) return
        finished = true

        action?.let(::track)
        router.finishBittensorFlow(exit)
    }

    private fun track(action: StakingResultAction) {
        if (trackActions) analytics.track(BittensorAnalyticsEvent.ResultAction(action, flow, target))
    }
}
