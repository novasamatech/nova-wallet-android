package io.novafoundation.nova.app.root.navigation.navigators.staking.relaychain

import android.annotation.SuppressLint
import androidx.annotation.IdRes
import androidx.navigation.NavController
import io.novafoundation.nova.app.R
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFlowExit

/** Screens a Bittensor operation starts from and returns to; they outlive the operation. */
private val BITTENSOR_FLOW_HOSTS = setOf(
    R.id.mainFragment,
    R.id.bittensorPortfolioFragment,
    R.id.bittensorRootDetailsFragment,
    R.id.bittensorSubnetPosition
)

/** Screens of an operation in progress (iOS presents them as a modal flow). */
private val BITTENSOR_FLOW_SCREENS = setOf(
    R.id.startStakingLandingFragment,
    R.id.bittensorWhereToStake,
    R.id.bittensorChooseSubnet,
    R.id.bittensorSubnetDetails,
    R.id.bittensorSelectValidator,
    R.id.bittensorSubnetAmount,
    R.id.bittensorSubnetReview,
    R.id.bittensorStakeAmountFragment,
    R.id.bittensorStakeConfirmFragment,
    R.id.bittensorSelectUnstakePosition,
    R.id.bittensorExitAmount,
    R.id.bittensorExitReview,
    R.id.bittensorRootClaim
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

/** A staking deep link must not tear down an operation the user is in the middle of (iOS `canPresentScreenWithoutBreakingFlow`). */
internal fun isBittensorFlowOpen(backStack: List<Int>): Boolean = backStack.any { it in BITTENSOR_FLOW_SCREENS }

@SuppressLint("RestrictedApi")
internal fun NavController.backStackDestinationIds(): List<Int> = backStack.map { it.destination.id }
