package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

class BittensorResultNavigationTest {

    private val router = mock(StakingRouter::class.java)

    private val analytics = RecordingBittensorAnalytics()

    private fun navigation(
        flow: StakingFlow,
        target: StakingAnalyticsTarget,
        newPosition: Boolean,
        trackActions: Boolean = true
    ) = BittensorResultNavigation(router, analytics, flow, target, newPosition, trackActions)

    private fun reportedActions() = analytics.events.filter { it.name == "staking_result_action" }.map { it.properties["action"] }

    @Test
    fun `new root stake view position opens the portfolio`() {
        navigation(StakingFlow.STAKE, StakingAnalyticsTarget.ROOT, newPosition = true).viewPosition()

        verify(router).finishBittensorFlow(BittensorFlowExit.PORTFOLIO)
        assertEquals(listOf("view_position"), reportedActions())
    }

    @Test
    fun `root stake more view position returns to root details`() {
        navigation(StakingFlow.STAKE, StakingAnalyticsTarget.ROOT, newPosition = false).viewPosition()

        verify(router).finishBittensorFlow(BittensorFlowExit.ORIGIN)
        assertEquals(listOf("back_to_position"), reportedActions())
    }

    @Test
    fun `new subnet position done opens the portfolio`() {
        navigation(StakingFlow.STAKE, StakingAnalyticsTarget.SUBNET, newPosition = true).done()

        verify(router).finishBittensorFlow(BittensorFlowExit.PORTFOLIO)
        assertEquals(listOf("your_bittensor"), reportedActions())
    }

    @Test
    fun `buy more done returns to the position`() {
        navigation(StakingFlow.STAKE, StakingAnalyticsTarget.SUBNET, newPosition = false).done()

        verify(router).finishBittensorFlow(BittensorFlowExit.ORIGIN)
        assertEquals(listOf("close"), reportedActions())
    }

    @Test
    fun `full exit done opens the portfolio and partial exit returns to the position`() {
        navigation(StakingFlow.UNSTAKE, StakingAnalyticsTarget.SUBNET, newPosition = false).done(emptiesPosition = true)
        navigation(StakingFlow.UNSTAKE, StakingAnalyticsTarget.SUBNET, newPosition = false).done(emptiesPosition = false)

        verify(router).finishBittensorFlow(BittensorFlowExit.PORTFOLIO)
        verify(router).finishBittensorFlow(BittensorFlowExit.ORIGIN)
        assertEquals(listOf("your_bittensor", "close"), reportedActions())
    }

    @Test
    fun `full root unstake view position goes to the portfolio`() {
        navigation(StakingFlow.UNSTAKE, StakingAnalyticsTarget.ROOT, newPosition = false).viewPosition(emptiesPosition = true)

        verify(router).finishBittensorFlow(BittensorFlowExit.PORTFOLIO)
        assertEquals(listOf("your_bittensor"), reportedActions())
    }

    @Test
    fun `claim returns to root details`() {
        navigation(StakingFlow.CLAIM, StakingAnalyticsTarget.ROOT, newPosition = false).viewPosition()

        verify(router).finishBittensorFlow(BittensorFlowExit.ORIGIN)
    }

    @Test
    fun `back or dismiss after a finished operation leaves like done without reporting an action`() {
        navigation(StakingFlow.UNSTAKE, StakingAnalyticsTarget.SUBNET, newPosition = false).leave(emptiesPosition = true)
        navigation(StakingFlow.STAKE, StakingAnalyticsTarget.SUBNET, newPosition = false).leave()

        verify(router).finishBittensorFlow(BittensorFlowExit.PORTFOLIO)
        verify(router).finishBittensorFlow(BittensorFlowExit.ORIGIN)
        assertEquals(emptyList<Any>(), reportedActions())
    }

    @Test
    fun `sheet dismissal after a button tap does not navigate twice`() {
        val navigation = navigation(StakingFlow.STAKE, StakingAnalyticsTarget.ROOT, newPosition = true)

        navigation.discoverSubnets("Bittensor")
        navigation.leave()
        navigation.viewPosition()

        verify(router, times(1)).finishBittensorFlow(BittensorFlowExit.SUBNET_DISCOVERY)
        verifyNoMoreInteractions(router)
        assertEquals(listOf("discover_subnets"), reportedActions())
        val opened = analytics.events.single { it.name == "staking_flow_opened" }
        assertEquals("result_discover", opened.properties["source"])
    }

    @Test
    fun `preview runs navigate without analytics`() {
        navigation(StakingFlow.CLAIM, StakingAnalyticsTarget.ROOT, newPosition = false, trackActions = false).done()

        verify(router).finishBittensorFlow(BittensorFlowExit.ORIGIN)
        assertEquals(emptyList<Any>(), analytics.events)
    }
}
