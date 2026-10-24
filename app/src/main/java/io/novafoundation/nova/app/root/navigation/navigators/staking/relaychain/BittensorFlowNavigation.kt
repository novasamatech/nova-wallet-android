package io.novafoundation.nova.app.root.navigation.navigators.staking.relaychain

import android.annotation.SuppressLint
import androidx.annotation.IdRes
import androidx.navigation.FloatingWindow
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
 * Browse screens a staking deep link may pop, nested graphs included: iOS pushes them inside a tab and pops the staking
 * tab to its root. Anything else on the split screen back stack (a setup or confirmation, Send, Swap, signing...)
 * is a flow in progress the link must not interrupt.
 */
private val STAKING_DEEP_LINK_BROWSE_SCREENS = setOf(
    R.id.split_screen_nav_graph,
    R.id.mainFragment,
    // Assets, staking and Your Bittensor
    R.id.balanceDetailFragment,
    R.id.staking_main_graph,
    R.id.stakingFragment,
    R.id.bittensorPortfolioFragment,
    R.id.bittensorRootDetailsFragment,
    R.id.bittensorSubnetPosition,
    R.id.bittensorValidatorInfo,
    R.id.nfts_nav_graph,
    R.id.nftListFragment,
    R.id.nftDetailsFragment,
    R.id.nova_card_graph,
    R.id.novaCardFragment,
    // Governance and crowdloans
    R.id.referendum_details_graph,
    R.id.referendumDetailsFragment,
    R.id.delegation_nav_graph,
    R.id.delegateListFragment,
    R.id.delegateSearchFragment,
    R.id.delegation_details_nav_graph,
    R.id.delegateDetailsFragment,
    R.id.votedReferendaFragment,
    R.id.delegateFullDescription,
    R.id.delegateDelegatorsFragment,
    R.id.your_delegations_nav_graph,
    R.id.yourDelegationsFragment,
    R.id.selectGovernanceTracks,
    R.id.tinder_gov_graph,
    R.id.tinderGovCards,
    R.id.crowdloan_contributions_graph,
    R.id.userContributionsFragment,
    // Wallets, settings, backups and networks
    R.id.accountsFragment,
    R.id.walletDetailsFragment,
    R.id.selectMultipleWalletsFragment,
    R.id.selectSingleWalletFragment,
    R.id.exportSeedFragment,
    R.id.multisig_pending_operations_graph,
    R.id.multisigPendingOperationsFragment,
    R.id.selectCurrencyFragment,
    R.id.languagesFragment,
    R.id.appearanceFragment,
    R.id.privacyFragment,
    R.id.authorizedDAppsFragment,
    R.id.wallet_connect_nav_graph,
    R.id.walletConnectSessionsFragment,
    R.id.walletConnectSessionDetailsFragment,
    R.id.push_settings_graph,
    R.id.pushSettingsFragment,
    R.id.pushMultisigFragment,
    R.id.pushGovernanceFragment,
    R.id.pushStakingSettings,
    R.id.cloud_backup_settings_graph,
    R.id.cloudBackupSettingsFragment,
    R.id.manual_backup_graph,
    R.id.manualBackupSelectWallet,
    R.id.manualBackupSelectAccount,
    R.id.manualBackupWarning,
    R.id.manualBackupSecrets,
    R.id.manualBackupAdvancedSecrets,
    R.id.network_management_graph,
    R.id.networkManagement,
    R.id.preConfiguredNetworks,
    R.id.networkManagementDetails,
    R.id.nodesFragment,
    R.id.nodeDetailsFragment
)

internal sealed interface BittensorFlowExitPlan {

    /** Pops everything above [destination], then optionally continues to the "Where to stake" quick pick. */
    data class PopTo(@IdRes val destination: Int, val openWhereToStake: Boolean = false) : BittensorFlowExitPlan

    /** The portfolio is on the back stack: pop back to it and let it report its opening again, as iOS `reopen(from: .result)`. */
    data object ReturnToPortfolio : BittensorFlowExitPlan

    /** The flow did not start from the portfolio: replace the whole flow with the portfolio over the dashboard. */
    data object OpenPortfolioOverDashboard : BittensorFlowExitPlan
}

/** Mirrors iOS `SubtensorOperationResultWireframe` on Android's single back stack, given its destination ids bottom to top. */
internal fun planBittensorFlowExit(exit: BittensorFlowExit, backStack: List<Int>): BittensorFlowExitPlan {
    val origin = backStack.lastOrNull { it in BITTENSOR_FLOW_HOSTS } ?: R.id.mainFragment

    return when (exit) {
        BittensorFlowExit.PORTFOLIO -> if (R.id.bittensorPortfolioFragment in backStack) {
            BittensorFlowExitPlan.ReturnToPortfolio
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
 * not while a root screen covers the split screen (dApp browser, external signing, WalletConnect approval...) or anything
 * but browse screens is open in it. New screens count as a flow until they join the browse screens.
 * [rootScreen] is the root graph's current destination. [splitScreenBackStack] holds the split screen's destination ids
 * bottom to top, nested graphs included, or is null while the split screen's view is not attached yet.
 */
internal fun canPresentStakingDeepLink(rootScreen: Int?, splitScreenBackStack: List<Int>?): Boolean {
    if (rootScreen != R.id.splitScreenFragment) return false
    val backStack = splitScreenBackStack ?: return true

    return backStack.all { it in STAKING_DEEP_LINK_BROWSE_SCREENS }
}

/** Screen ids on the back stack, bottom to top; nested graph entries are skipped. */
@SuppressLint("RestrictedApi")
internal fun NavController.backStackDestinationIds(): List<Int> = backStack
    .map { it.destination }
    .filterNot { it is NavGraph }
    .map { it.id }

/** Destination ids on the back stack, bottom to top, nested graph entries included. */
@SuppressLint("RestrictedApi")
internal fun NavController.backStackIdsWithGraphs(): List<Int> = backStack.map { it.destination.id }

/** A dialog destination is on top; iOS ignores the link under any presented sheet. */
internal fun NavController.isShowingDialog(): Boolean = currentDestination is FloatingWindow
