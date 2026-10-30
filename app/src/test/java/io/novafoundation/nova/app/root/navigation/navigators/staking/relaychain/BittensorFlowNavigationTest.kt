package io.novafoundation.nova.app.root.navigation.navigators.staking.relaychain

import io.novafoundation.nova.app.R
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFlowExit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorFlowNavigationTest {

    private val firstRootStake = listOf(
        R.id.mainFragment,
        R.id.startStakingLandingFragment,
        R.id.bittensorWhereToStake,
        R.id.bittensorStakeAmountFragment,
        R.id.bittensorStakeConfirmFragment
    )

    private val addPositionFromPortfolio = listOf(
        R.id.mainFragment,
        R.id.bittensorPortfolioFragment,
        R.id.startStakingLandingFragment,
        R.id.bittensorChooseSubnet,
        R.id.bittensorSubnetDetails,
        R.id.bittensorSubnetAmount,
        R.id.bittensorSubnetReview
    )

    private val sellFromPosition = listOf(
        R.id.mainFragment,
        R.id.bittensorPortfolioFragment,
        R.id.bittensorSubnetPosition,
        R.id.bittensorExitAmount,
        R.id.bittensorExitReview
    )

    private val claimFromRootDetails = listOf(
        R.id.mainFragment,
        R.id.bittensorPortfolioFragment,
        R.id.bittensorRootDetailsFragment,
        R.id.bittensorRootClaim
    )

    @Test
    fun `first stake opens the portfolio over the dashboard instead of stacking it on the flow`() {
        val plan = planBittensorFlowExit(BittensorFlowExit.PORTFOLIO, firstRootStake)

        assertEquals(BittensorFlowExitPlan.OpenPortfolioOverDashboard, plan)
    }

    @Test
    fun `re-entry from the portfolio pops back to it`() {
        val plan = planBittensorFlowExit(BittensorFlowExit.PORTFOLIO, addPositionFromPortfolio)

        assertEquals(BittensorFlowExitPlan.ReturnToPortfolio, plan)
    }

    @Test
    fun `emptied position pops past the stale position screen to the portfolio`() {
        val plan = planBittensorFlowExit(BittensorFlowExit.PORTFOLIO, sellFromPosition)

        assertEquals(BittensorFlowExitPlan.ReturnToPortfolio, plan)
    }

    @Test
    fun `partial sell returns to the position it started from`() {
        val plan = planBittensorFlowExit(BittensorFlowExit.ORIGIN, sellFromPosition)

        assertEquals(BittensorFlowExitPlan.PopTo(R.id.bittensorSubnetPosition), plan)
    }

    @Test
    fun `claim returns to root details`() {
        val plan = planBittensorFlowExit(BittensorFlowExit.ORIGIN, claimFromRootDetails)

        assertEquals(BittensorFlowExitPlan.PopTo(R.id.bittensorRootDetailsFragment), plan)
    }

    @Test
    fun `origin of a flow started from the landing is the screen below the landing`() {
        assertEquals(BittensorFlowExitPlan.PopTo(R.id.mainFragment), planBittensorFlowExit(BittensorFlowExit.ORIGIN, firstRootStake))
        assertEquals(
            BittensorFlowExitPlan.PopTo(R.id.bittensorPortfolioFragment),
            planBittensorFlowExit(BittensorFlowExit.ORIGIN, addPositionFromPortfolio)
        )
    }

    @Test
    fun `subnet discovery returns to the landing and continues to the quick pick`() {
        val plan = planBittensorFlowExit(BittensorFlowExit.SUBNET_DISCOVERY, firstRootStake)

        assertEquals(BittensorFlowExitPlan.PopTo(R.id.startStakingLandingFragment, openWhereToStake = true), plan)
    }

    @Test
    fun `subnet discovery without a landing returns to the flow origin first`() {
        val plan = planBittensorFlowExit(BittensorFlowExit.SUBNET_DISCOVERY, claimFromRootDetails)

        assertEquals(BittensorFlowExitPlan.PopTo(R.id.bittensorRootDetailsFragment, openWhereToStake = true), plan)
    }

    @Test
    fun `sell from a position opened without the portfolio`() {
        val sellFromWalletPosition = listOf(
            R.id.mainFragment,
            R.id.balanceDetailFragment,
            R.id.bittensorSubnetPosition,
            R.id.bittensorExitAmount,
            R.id.bittensorExitReview
        )

        assertEquals(BittensorFlowExitPlan.OpenPortfolioOverDashboard, planBittensorFlowExit(BittensorFlowExit.PORTFOLIO, sellFromWalletPosition))
        assertEquals(BittensorFlowExitPlan.PopTo(R.id.bittensorSubnetPosition), planBittensorFlowExit(BittensorFlowExit.ORIGIN, sellFromWalletPosition))
    }

    @Test
    fun `staking deep link is ignored while any flow is open`() {
        val firstStakeWithGraphs = listOf(
            R.id.split_screen_nav_graph,
            R.id.mainFragment,
            R.id.start_staking_nav_graph,
            R.id.startStakingLandingFragment,
            R.id.bittensorWhereToStake,
            R.id.bittensor_root_staking_graph,
            R.id.bittensorStakeAmountFragment
        )

        assertFalse(canPresentOverSplitScreen(firstStakeWithGraphs))
        assertFalse(canPresentOverSplitScreen(claimFromRootDetails))
        assertFalse(canPresentOverSplitScreen(listOf(R.id.mainFragment, R.id.staking_main_graph, R.id.stakingFragment, R.id.selectBondMoreFragment)))
        assertFalse(canPresentOverSplitScreen(listOf(R.id.mainFragment, R.id.sendFlowFragment)))
        assertFalse(canPresentOverSplitScreen(listOf(R.id.mainFragment, R.id.start_swap_nav_graph, R.id.swapSettingsFragment)))
    }

    @Test
    fun `staking deep link is ignored over screens iOS presents modally`() {
        assertFalse(canPresentOverSplitScreen(listOf(R.id.mainFragment, R.id.balanceDetailFragment, R.id.transferDetailFragment)))
        assertFalse(canPresentOverSplitScreen(listOf(R.id.mainFragment, R.id.manage_tokens_graph, R.id.manageTokensFragment)))
        assertFalse(canPresentOverSplitScreen(listOf(R.id.split_screen_nav_graph, R.id.mainFragment, R.id.switchWalletFragment)))
        assertFalse(
            canPresentOverSplitScreen(listOf(R.id.mainFragment, R.id.referendum_details_graph, R.id.referendumDetailsFragment, R.id.referendumVoters))
        )
    }

    @Test
    fun `staking deep link replaces browse screens iOS pushes inside a tab`() {
        assertTrue(canPresentOverSplitScreen(listOf(R.id.split_screen_nav_graph, R.id.mainFragment)))
        assertTrue(canPresentOverSplitScreen(listOf(R.id.mainFragment, R.id.bittensorPortfolioFragment, R.id.bittensorSubnetPosition)))
        assertTrue(canPresentOverSplitScreen(listOf(R.id.mainFragment, R.id.balanceDetailFragment, R.id.bittensorPortfolioFragment)))
        assertTrue(canPresentOverSplitScreen(listOf(R.id.mainFragment, R.id.staking_main_graph, R.id.stakingFragment)))
        assertTrue(canPresentOverSplitScreen(listOf(R.id.mainFragment, R.id.referendum_details_graph, R.id.referendumDetailsFragment)))
        assertTrue(canPresentOverSplitScreen(listOf(R.id.mainFragment, R.id.accountsFragment, R.id.walletDetailsFragment)))
        assertTrue(canPresentOverSplitScreen(listOf(R.id.mainFragment, R.id.nfts_nav_graph, R.id.nftListFragment, R.id.nftDetailsFragment)))
    }

    @Test
    fun `staking deep link is ignored while a root screen covers the split screen`() {
        assertFalse(canPresentStakingDeepLink(R.id.dappBrowserFragment, null))
        assertFalse(canPresentStakingDeepLink(R.id.ConfirmSignExtrinsicFragment, null))
        assertFalse(canPresentStakingDeepLink(R.id.walletConnectApproveSessionFragment, null))
        assertFalse(canPresentStakingDeepLink(R.id.selectWalletFragment, listOf(R.id.split_screen_nav_graph, R.id.mainFragment)))
    }

    @Test
    fun `staking deep link resets a split screen whose view is not attached yet`() {
        assertTrue(canPresentStakingDeepLink(R.id.splitScreenFragment, null))
    }

    private fun canPresentOverSplitScreen(splitScreenBackStack: List<Int>) = canPresentStakingDeepLink(R.id.splitScreenFragment, splitScreenBackStack)
}
