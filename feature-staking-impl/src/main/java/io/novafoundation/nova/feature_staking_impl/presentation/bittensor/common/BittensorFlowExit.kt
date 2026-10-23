package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.StakingResultAction

/**
 * Where a finished Bittensor operation leaves the user. Every exit drops the flow screens from the back stack,
 * so Back never returns to a stale setup or review.
 */
enum class BittensorFlowExit {

    /** "Your Bittensor": back to the portfolio, opened over the dashboard when the flow did not start from it. */
    PORTFOLIO,

    /** Back to the screen the flow started from: the position, root details, the portfolio or the dashboard. */
    ORIGIN,

    /** Back to the Earn landing (or the flow origin when there is none) and on to the "Where to stake" quick pick. */
    SUBNET_DISCOVERY
}

/**
 * "Done" after a completed or unconfirmed operation, as iOS `SubtensorOperationResultPresenter.completeAfterSuccess`:
 * a new position or an emptied one goes to the portfolio, anything else returns to where the flow started.
 */
fun bittensorCompletedFlowExit(newPosition: Boolean, emptiesPosition: Boolean): BittensorFlowExit {
    return if (newPosition || emptiesPosition) BittensorFlowExit.PORTFOLIO else BittensorFlowExit.ORIGIN
}

/** Result action reported for "Done", as iOS `SubtensorOperationResultPresenter.analyticsAction(for: .done)`. */
val BittensorFlowExit.doneAnalyticsAction: StakingResultAction
    get() = when (this) {
        BittensorFlowExit.PORTFOLIO -> StakingResultAction.YOUR_BITTENSOR
        BittensorFlowExit.ORIGIN -> StakingResultAction.CLOSE
        BittensorFlowExit.SUBNET_DISCOVERY -> StakingResultAction.DISCOVER_SUBNETS
    }
