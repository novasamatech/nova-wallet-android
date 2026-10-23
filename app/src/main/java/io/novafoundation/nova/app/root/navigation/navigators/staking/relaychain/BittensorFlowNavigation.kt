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
 * Nested graphs that are a flow as a whole: iOS presents them modally over the tab bar, or keeps them in their own tab
 * where Android's single back stack would tear them down.
 */
private val STAKING_DEEP_LINK_FLOW_GRAPHS = setOf(
    R.id.onboarding_nav_graph,
    R.id.import_nav_graph,
    R.id.import_wallet_options_nav_graph,
    R.id.create_wallet_nav_graph,
    R.id.mnemonic_nav_graph,
    R.id.add_ledger_chain_account_graph,
    R.id.add_evm_account_generic_ledger_graph,
    R.id.start_staking_nav_graph,
    R.id.start_multi_staking_nav_graph,
    R.id.setup_staking_validators_graph,
    R.id.staking_parachain_start_graph,
    R.id.staking_parachain_unbond,
    R.id.staking_parachain_yield_boost,
    R.id.staking_mythos_start_graph,
    R.id.staking_mythos_unbond_graph,
    R.id.nomination_pools_bond_more_graph,
    R.id.nomination_pools_unbond_graph,
    R.id.bittensor_root_staking_graph,
    R.id.start_swap_nav_graph,
    R.id.select_swap_token_nav_graph,
    R.id.manage_tokens_graph,
    R.id.gifts_nav_graph,
    R.id.referendum_unlock_graph,
    R.id.referenda_search_graph,
    R.id.dapp_browser_graph,
    R.id.dapp_search_graph,
    R.id.external_sign_graph,
    R.id.sign_ledger_nav_graph,
    R.id.sign_parity_signer_graph,
    R.id.multisig_operation_details_graph
)

/** Flow screens in graphs that also hold browse screens (chain staking, referendum details, delegations, settings). */
private val STAKING_DEEP_LINK_FLOW_SCREENS = setOf(
    // Everything chain staking opens; iOS presents all of it modally over staking main
    R.id.validatorDetailsFragment,
    R.id.stakingPeriods,
    R.id.startChangeValidatorsFragment,
    R.id.recommendedValidatorsFragment,
    R.id.confirmStakingFragment,
    R.id.confirmNominationsFragment,
    R.id.currentValidatorsFragment,
    R.id.currentCollatorsFragment,
    R.id.mythosCurrentCollatorsFragment,
    R.id.payoutsListFragment,
    R.id.payoutDetailsFragment,
    R.id.confirmPayoutFragment,
    R.id.setControllerAccountFragment,
    R.id.confirmSetControllerAccount,
    R.id.selectBondMoreFragment,
    R.id.confirmBondMoreFragment,
    R.id.selectUnbondFragment,
    R.id.confirmUnbondFragment,
    R.id.redeemFragment,
    R.id.confirmRebondFragment,
    R.id.customRebondFragment,
    R.id.selectRewardDestinationFragment,
    R.id.confirmRewardDestinationFragment,
    R.id.rebagFragment,
    R.id.parachainStakingRedeemFragment,
    R.id.parachainStakingRebondFragment,
    R.id.nominationPoolsRedeemFragment,
    R.id.nominationPoolsClaimRewardsFragment,
    R.id.mythosRedeemFragment,
    R.id.mythosClaimRewardsFragment,
    R.id.stakingProxyListFragment,
    R.id.addStakingProxyFragment,
    R.id.confirmAddStakingProxyFragment,
    R.id.confirmRemoveStakingProxyFragment,
    // Bittensor operations
    R.id.bittensorWhereToStake,
    R.id.bittensorChooseSubnet,
    R.id.bittensorSubnetDetails,
    R.id.bittensorSelectValidator,
    R.id.bittensorSubnetAmount,
    R.id.bittensorSubnetReview,
    R.id.bittensorSelectUnstakePosition,
    R.id.bittensorExitAmount,
    R.id.bittensorExitReview,
    R.id.bittensorRootClaim,
    // Send, receive, buy, sell, gifts and transaction details
    R.id.assetSearchFragment,
    R.id.sendFlowFragment,
    R.id.sendFlowNetworkFragment,
    R.id.selectSendFragment,
    R.id.confirmTransferFragment,
    R.id.receiveFlowFragment,
    R.id.receiveFlowNetworkFragment,
    R.id.receiveFragment,
    R.id.buyFlowFragment,
    R.id.buyFlowNetworkFragment,
    R.id.sellFlowFragment,
    R.id.sellFlowNetworkFragment,
    R.id.tradeProvidersFragment,
    R.id.tradeWebFragment,
    R.id.topUpAddressFragment,
    R.id.claimGiftFragment,
    R.id.transactionHistoryFilterFragment,
    R.id.transferDetailFragment,
    R.id.extrinsicDetailFragment,
    R.id.rewardDetailFragment,
    R.id.poolRewardDetailFragment,
    R.id.swapDetailFragment,
    // Governance
    R.id.referendaFiltersFragment,
    R.id.referendumFullDetailsFragment,
    R.id.referendumDescription,
    R.id.referendumVoters,
    R.id.setupVoteReferendumFragment,
    R.id.confirmReferendumVote,
    R.id.selectDelegationTracks,
    R.id.removeVoteFragment,
    R.id.newDelegationChooseAmountFragment,
    R.id.newDelegationConfirmFragment,
    R.id.revokeDelegationChooseTracksFragment,
    R.id.revokeDelegationConfirmFragment,
    R.id.referendumInfoFragment,
    R.id.setupTinderGovVoteFragment,
    R.id.setupTinderGovBasketFragment,
    R.id.confirmTinderGovVoteFragment,
    R.id.crowdloanContributeFragment,
    R.id.customContributeFragment,
    R.id.moonbeamCrowdloanTermsFragment,
    R.id.confirmContributeFragment,
    R.id.claimContributionFragment,
    // Wallets, security, backups and networks
    R.id.pincodeFragment,
    R.id.advancedEncryptionFragment,
    R.id.scanSeedFragment,
    R.id.changeWatchAccountFragment,
    R.id.exportJsonPasswordFragment,
    R.id.syncWalletsBackupPassword,
    R.id.restoreCloudBackupPasswordFragment,
    R.id.checkCloudBackupPasswordFragment,
    R.id.changeBackupPasswordFragment,
    R.id.addNetworkFragment,
    R.id.customNode,
    R.id.addNodeFragment,
    // dApps, WalletConnect and notifications
    R.id.dappFavorites,
    R.id.walletConnectScanFragment,
    R.id.walletConnectApproveSessionFragment,
    R.id.pushNotificationsWelcomeFragment
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
 * not while a root screen covers the split screen (dApp browser, external signing, WalletConnect approval...) or a flow
 * is open in it. Browse screens iOS pushes inside a tab (asset details, chain staking, Your Bittensor, referendum details,
 * settings...) are no flow: the link pops them, as iOS pops the staking tab to its root.
 * [rootScreen] is the root graph's current destination. [splitScreenBackStack] holds the split screen's destination ids
 * bottom to top, nested graphs included, or is null while the split screen's view is not attached yet.
 */
internal fun canPresentStakingDeepLink(rootScreen: Int?, splitScreenBackStack: List<Int>?): Boolean {
    if (rootScreen != R.id.splitScreenFragment) return false
    val backStack = splitScreenBackStack ?: return true

    return backStack.none { it in STAKING_DEEP_LINK_FLOW_GRAPHS || it in STAKING_DEEP_LINK_FLOW_SCREENS }
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
