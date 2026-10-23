package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.StakingResultAction
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorFlowExitTest {

    @Test
    fun `new position finishes on the portfolio`() {
        assertEquals(BittensorFlowExit.PORTFOLIO, bittensorCompletedFlowExit(newPosition = true, emptiesPosition = false))
    }

    @Test
    fun `emptied position finishes on the portfolio`() {
        assertEquals(BittensorFlowExit.PORTFOLIO, bittensorCompletedFlowExit(newPosition = false, emptiesPosition = true))
    }

    @Test
    fun `operation on a kept position returns to where it started`() {
        assertEquals(BittensorFlowExit.ORIGIN, bittensorCompletedFlowExit(newPosition = false, emptiesPosition = false))
    }

    @Test
    fun `done is reported by where it leads`() {
        assertEquals(StakingResultAction.YOUR_BITTENSOR, BittensorFlowExit.PORTFOLIO.doneAnalyticsAction)
        assertEquals(StakingResultAction.CLOSE, BittensorFlowExit.ORIGIN.doneAnalyticsAction)
        assertEquals(StakingResultAction.DISCOVER_SUBNETS, BittensorFlowExit.SUBNET_DISCOVERY.doneAnalyticsAction)
    }
}
