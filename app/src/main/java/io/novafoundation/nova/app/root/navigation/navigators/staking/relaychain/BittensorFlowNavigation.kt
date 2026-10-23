package io.novafoundation.nova.app.root.navigation.navigators.staking.relaychain

import android.annotation.SuppressLint
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import io.novafoundation.nova.app.R
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFlowExit

/** Screens a Bittensor operation starts from and returns to; they outlive the operation. */
private val BITTENSOR_FLOW_HOSTS = setOf(
    R.id.mainFragment,
    R.id.bittensorPortfolioFragment,
    R.id.bittensorRootDetailsFragment,
    R.id.bittensorSubnetPosition
)

/**
 * Browse screens a staking deep link may replace. Anything else above the main screen is a flow in progress
 * (a staking setup or confirmation, Send, Swap...), which iOS keeps modal and never interrupts.
 */
private val STAKING_DEEP_LINK_REPLACEABLE_SCREENS = setOf(
    R.id.bittensorPortfolioFragment,
    R.id.bittensorRootDetailsFragment,
    R.id.bittensorSubnetPosition,
    R.id.stakingFragment,
    R.id.balanceDetailFragment
)

internal sealed interface BittensorFlowExitPlan {

    /** Pops everything above [destination], then optionally continues to the "Where to stake" quick pick. */
    data class PopTo(@IdRes val destination: Int, val openWhereToStake: Boolean = false) : BittensorFlowExitPlan

    /** The flow did not start from the portfolio: replace the whole flow with the portfolio over the dashboard. */
    data object OpenPortfolioOverDashboard : BittensorFlowExitPlan
}

/** Mirrors iOS `SubtensorOperationResultWireframe` on Android's single back stack, given its destination ids bottom to top. */
internal fun planBittensorFlowExit(exit: BittensorFlowExit, backStack: List<Int>): BittensorFlowExitPlan {
    val origin = backStack.lastOrNull { it in BITTENSOR_FLOW_HOSTS } ?: R.id.mainFragment

    return when (exit) {
        BittensorFlowExit.PORTFOLIO -> if (R.id.bittensorPortfolioFragment in backStack) {
            BittensorFlowExitPlan.PopTo(R.id.bittensorPortfolioFragment)
        } else {
            BittensorFlowExitPlan.OpenPortfolioOverDashboard
        }

        BittensorFlowExit.ORIGIN -> BittensorFlowExitPlan.PopTo(origin)

        BittensorFlowExit.SUBNET_DISCOVERY -> {
            val landing = R.id.startStakingLandingFragment.takeIf { it in backStack.dropWhile { id -> id != origin } }
            BittensorFlowExitPlan.PopTo(landing ?: origin, openWhereToStake = true)
        }
    }
}

/**
 * Whether a staking deep link may reset the split screen to the dashboard, as iOS `canPresentScreenWithoutBreakingFlow`:
 * only when nothing above the main screen is a flow in progress. [backStack] holds screen ids, bottom to top.
 */
internal fun canPresentStakingDeepLink(backStack: List<Int>): Boolean {
    val aboveMain = backStack.dropWhile { it != R.id.mainFragment }.drop(1)
    return aboveMain.all { it in STAKING_DEEP_LINK_REPLACEABLE_SCREENS }
}

/** Screen ids on the back stack, bottom to top; nested graph entries are skipped. */
@SuppressLint("RestrictedApi")
internal fun NavController.backStackDestinationIds(): List<Int> = backStack
    .map { it.destination }
    .filterNot { it is NavGraph }
    .map { it.id }
