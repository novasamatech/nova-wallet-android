package io.novafoundation.nova.app.root.navigation.navigators.staking.relaychain

import io.novafoundation.nova.app.R
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFlowExit
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorFlowNavigationTest {

    private val firstRootStake = listOf(
        R.id.split_screen_nav_graph,
        R.id.mainFragment,
        R.id.start_staking_nav_graph,
        R.id.startStakingLandingFragment,
        R.id.bittensorWhereToStake,
        R.id.bittensor_root_staking_graph,
        R.id.bittensorStakeAmountFragment,
        R.id.bittensorStakeConfirmFragment
    )

    private val addPositionFromPortfolio = listOf(
        R.id.split_screen_nav_graph,
        R.id.mainFragment,
        R.id.bittensorPortfolioFragment,
        R.id.start_staking_nav_graph,
        R.id.startStakingLandingFragment,
        R.id.bittensorChooseSubnet,
        R.id.bittensorSubnetDetails,
        R.id.bittensorSubnetAmount,
        R.id.bittensorSubnetReview
    )

    private val sellFromPosition = listOf(
        R.id.split_screen_nav_graph,
        R.id.mainFragment,
        R.id.bittensorPortfolioFragment,
        R.id.bittensorSubnetPosition,
        R.id.bittensorExitAmount,
        R.id.bittensorExitReview
    )

    private val claimFromRootDetails = listOf(
        R.id.split_screen_nav_graph,
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

        assertEquals(BittensorFlowExitPlan.PopTo(R.id.bittensorPortfolioFragment), plan)
    }

    @Test
    fun `emptied position pops past the stale position screen to the portfolio`() {
        val plan = planBittensorFlowExit(BittensorFlowExit.PORTFOLIO, sellFromPosition)

        assertEquals(BittensorFlowExitPlan.PopTo(R.id.bittensorPortfolioFragment), plan)
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
}
