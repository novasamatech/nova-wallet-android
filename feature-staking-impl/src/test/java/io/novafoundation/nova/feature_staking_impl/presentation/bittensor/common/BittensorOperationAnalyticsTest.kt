package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.analytics.StakingMode
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorOperationAnalyticsTest {
    private val service = RecordingBittensorAnalytics()

    private val analytics = BittensorOperationAnalytics(service)

    @Test
    fun `new attempts are separate events and independent screens do not consume each other`() {
        val otherScreen = BittensorOperationAnalytics(service)
        val result = BittensorOperationResult.Success("hash")
        analytics.startOperation()
        otherScreen.startOperation()
        analytics.outcome(result, StakingAnalyticsTarget.ROOT, StakingFlow.CLAIM)
        otherScreen.outcome(result, StakingAnalyticsTarget.ROOT, StakingFlow.CLAIM)
        analytics.startOperation()
        analytics.outcome(result, StakingAnalyticsTarget.ROOT, StakingFlow.CLAIM)

        assertEquals(3, service.events.size)
    }

    @Test
    fun `unknown claim never emits claim completed`() {
        analytics.startOperation()
        analytics.outcome(BittensorOperationResult.Unknown("private hash"), StakingAnalyticsTarget.ROOT, StakingFlow.CLAIM)
        assertTrue(service.events.isEmpty())
    }

    @Test
    fun `unknown stake is unconfirmed and reported once without transaction hash`() {
        val result = BittensorOperationResult.Unknown("private hash")
        analytics.startOperation()
        repeat(2) {
            analytics.outcome(result, StakingAnalyticsTarget.SUBNET, StakingFlow.STAKE, StakingMode.NEW_POSITION)
        }
        val event = service.events.single()
        assertEquals("staking_completed", event.name)
        assertEquals("unconfirmed", event.properties["outcome"])
        assertFalse(event.properties.toString().contains("private hash"))
    }

    @Test
    fun `claim without executed amounts does not invent claimed zero`() {
        analytics.startOperation()
        analytics.outcome(BittensorOperationResult.Success("hash"), StakingAnalyticsTarget.ROOT, StakingFlow.CLAIM)
        val event = service.events.single()
        assertEquals("rewards_claim_completed", event.name)
        assertFalse(event.properties.containsKey("claimed_zero"))
    }

    @Test
    fun `disabled analytics respects service consent`() {
        service.isEnabled = false
        analytics.startOperation()
        analytics.outcome(BittensorOperationResult.Success("hash"), StakingAnalyticsTarget.ROOT, StakingFlow.CLAIM)
        assertTrue(service.events.isEmpty())
    }
}
