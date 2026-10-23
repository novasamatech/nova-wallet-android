package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.analytics.StakingFlowSource
import io.novafoundation.nova.analytics.StakingResultAction
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter

/**
 * Leaves a Bittensor confirmation screen once its operation is done or unconfirmed, as iOS
 * `SubtensorOperationResultPresenter.activate(action:)` and `goBack()`. One instance per screen: the first exit wins,
 * so a sheet dismissal that follows a button tap does not navigate twice.
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

    /** Only offered on a new position, as iOS `landingActions`; [network] is the chain name for `staking_flow_opened`. */
    fun discoverSubnets(network: String) {
        if (finished) return
        if (trackActions) analytics.track(AnalyticsEvent.StakingFlowOpened(network, StakingFlowSource.RESULT_DISCOVER.value))
        finish(BittensorFlowExit.SUBNET_DISCOVERY, StakingResultAction.DISCOVER_SUBNETS)
    }

    /** Back or a dismissed result sheet after the operation finished: same destination as "Done", not reported as an action. */
    fun leave(emptiesPosition: Boolean = false) = finish(bittensorCompletedFlowExit(newPosition, emptiesPosition), action = null)

    private fun finish(exit: BittensorFlowExit, action: StakingResultAction?) {
        if (finished) return
        finished = true

        if (trackActions && action != null) {
            analytics.track(BittensorAnalyticsEvent.ResultAction(action, flow, target))
        }
        router.finishBittensorFlow(exit)
    }
}
