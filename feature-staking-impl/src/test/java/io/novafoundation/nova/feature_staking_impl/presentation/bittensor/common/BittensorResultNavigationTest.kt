package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorResultAction
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
    fun `new root stake offers discover subnets and view position which opens the portfolio`() {
        val navigation = navigation(StakingFlow.STAKE, StakingAnalyticsTarget.ROOT, newPosition = true)

        assertEquals(listOf(BittensorResultAction.DISCOVER_SUBNETS, BittensorResultAction.VIEW_POSITION), navigation.completedActions())
        assertEquals(listOf(BittensorResultAction.VIEW_POSITION), navigation.landingActions())

        navigation.activate(BittensorResultAction.VIEW_POSITION) {}

        verify(router).finishBittensorFlow(BittensorFlowExit.PORTFOLIO)
        assertEquals(listOf("view_position"), reportedActions())
    }

    @Test
    fun `kept root position offers your bittensor and back to position`() {
        val kept = navigation(StakingFlow.CLAIM, StakingAnalyticsTarget.ROOT, newPosition = false)

        assertEquals(listOf(BittensorResultAction.YOUR_BITTENSOR, BittensorResultAction.BACK_TO_POSITION), kept.completedActions())

        kept.activate(BittensorResultAction.YOUR_BITTENSOR) {}
        navigation(StakingFlow.CLAIM, StakingAnalyticsTarget.ROOT, newPosition = false).activate(BittensorResultAction.BACK_TO_POSITION) {}

        verify(router).finishBittensorFlow(BittensorFlowExit.PORTFOLIO)
        verify(router).finishBittensorFlow(BittensorFlowExit.ORIGIN)
        assertEquals(listOf("your_bittensor", "back_to_position"), reportedActions())
    }

    @Test
    fun `emptied root position offers only your bittensor`() {
        val navigation = navigation(StakingFlow.UNSTAKE, StakingAnalyticsTarget.ROOT, newPosition = false)

        assertEquals(listOf(BittensorResultAction.YOUR_BITTENSOR), navigation.landingActions(emptiesPosition = true))
    }

    @Test
    fun `failure close leaves the flow and try again stays on the screen`() {
        var retries = 0
        val navigation = navigation(StakingFlow.STAKE, StakingAnalyticsTarget.ROOT, newPosition = false)

        navigation.activate(BittensorResultAction.TRY_AGAIN) { retries++ }
        navigation.activate(BittensorResultAction.CLOSE) { retries++ }

        assertEquals(1, retries)
        verify(router).finishBittensorFlow(BittensorFlowExit.ORIGIN)
        verifyNoMoreInteractions(router)
        assertEquals(listOf("try_again", "close"), reportedActions())
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
    fun `back after a finished operation leaves like done without reporting an action`() {
        navigation(StakingFlow.UNSTAKE, StakingAnalyticsTarget.SUBNET, newPosition = false).leave(emptiesPosition = true)
        navigation(StakingFlow.STAKE, StakingAnalyticsTarget.SUBNET, newPosition = false).leave()

        verify(router).finishBittensorFlow(BittensorFlowExit.PORTFOLIO)
        verify(router).finishBittensorFlow(BittensorFlowExit.ORIGIN)
        assertEquals(emptyList<Any>(), reportedActions())
    }

    @Test
    fun `only the first exit navigates`() {
        var retries = 0
        val navigation = navigation(StakingFlow.STAKE, StakingAnalyticsTarget.ROOT, newPosition = true)

        navigation.activate(BittensorResultAction.DISCOVER_SUBNETS) {}
        navigation.leave()
        navigation.activate(BittensorResultAction.VIEW_POSITION) {}
        navigation.activate(BittensorResultAction.TRY_AGAIN) { retries++ }

        assertEquals(0, retries)
        verify(router, times(1)).finishBittensorFlow(BittensorFlowExit.SUBNET_DISCOVERY)
        verifyNoMoreInteractions(router)
        assertEquals(listOf("staking_result_action", "staking_flow_opened"), analytics.events.map { it.name })
        assertEquals(listOf("discover_subnets"), reportedActions())
        val opened = analytics.events.single { it.name == "staking_flow_opened" }
        assertEquals("result_discover", opened.properties["source"])
        assertEquals("Bittensor", opened.properties["network"])
    }

    @Test
    fun `preview runs navigate without analytics`() {
        val navigation = navigation(StakingFlow.CLAIM, StakingAnalyticsTarget.ROOT, newPosition = false, trackActions = false)

        navigation.activate(BittensorResultAction.TRY_AGAIN) {}
        navigation.done()

        verify(router).finishBittensorFlow(BittensorFlowExit.ORIGIN)
        assertEquals(emptyList<Any>(), analytics.events)
    }
}
