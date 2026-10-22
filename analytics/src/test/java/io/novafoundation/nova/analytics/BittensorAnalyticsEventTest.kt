package io.novafoundation.nova.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BittensorAnalyticsEventTest {

    @Test
    fun `selection retains wire name and string netuid`() {
        val event = BittensorAnalyticsEvent.TypeSelected(
            "Bittensor", StakingAnalyticsTarget.SUBNET, StakingAnalyticsSelectionMethod.PICKER_SUBNET, 42
        )

        assertEquals("staking_type_selected", event.name)
        assertEquals(
            mapOf(
                "staking_type" to "subtensor",
                "network" to "Bittensor",
                "target" to "subnet",
                "selection_method" to "picker_subnet",
                "netuid" to "42"
            ),
            event.properties
        )
    }

    @Test
    fun `optional staking properties are omitted or included without changing values`() {
        val withoutOptionals = BittensorAnalyticsEvent.Initiated(
            "Bittensor", StakingAnalyticsTarget.SUBNET, StakingMode.BUY_MORE, StakingValidatorSource.MANUAL, null, null
        )
        val withOptionals = BittensorAnalyticsEvent.Initiated(
            "Bittensor", StakingAnalyticsTarget.SUBNET, StakingMode.BUY_MORE, StakingValidatorSource.MANUAL,
            SlippageBucket.LOW, AmountBucket.UNDER_1
        )

        assertEquals("staking_initiated", withoutOptionals.name)
        assertFalse(withoutOptionals.properties.containsKey("slippage_bucket"))
        assertFalse(withoutOptionals.properties.containsKey("amount_bucket"))
        assertEquals(
            withoutOptionals.properties + mapOf("slippage_bucket" to "low", "amount_bucket" to "under_1"),
            withOptionals.properties
        )
    }

    @Test
    fun `unknown claimed amount is omitted but false is preserved`() {
        val unknown = BittensorAnalyticsEvent.ClaimCompleted("Bittensor", null, DurationBucket.UNDER_5S, null)
        val nonZero = BittensorAnalyticsEvent.ClaimCompleted("Bittensor", null, DurationBucket.UNDER_5S, false)

        assertEquals("rewards_claim_completed", unknown.name)
        assertEquals(
            mapOf("staking_type" to "subtensor", "network" to "Bittensor", "target" to "root", "duration_bucket" to "under_5s"),
            unknown.properties
        )
        assertEquals(unknown.properties + ("claimed_zero" to false), nonZero.properties)
    }

    @Test
    fun `failures retain flow names and mode precedence`() {
        val expectedNames = mapOf(
            StakingFlow.STAKE to "staking_failed",
            StakingFlow.UNSTAKE to "unstake_failed",
            StakingFlow.CLAIM to "rewards_claim_failed"
        )
        expectedNames.forEach { (flow, name) ->
            val event = BittensorAnalyticsEvent.Failed(
                flow, "Bittensor", StakingAnalyticsTarget.ROOT, null, null,
                BittensorTransactionFailureReason.NOT_SUBMITTED, StakingFailureStage.NOT_SUBMITTED, false
            )
            assertEquals(name, event.name)
            assertFalse(event.properties.containsKey("mode"))
            assertEquals(false, event.properties["retryable"])
        }

        val unstake = BittensorAnalyticsEvent.Failed(
            StakingFlow.UNSTAKE, "Bittensor", StakingAnalyticsTarget.ROOT, null, UnstakeMode.FULL,
            BittensorTransactionFailureReason.NOT_SUBMITTED, StakingFailureStage.NOT_SUBMITTED, true
        )
        val stake = BittensorAnalyticsEvent.Failed(
            StakingFlow.STAKE, "Bittensor", StakingAnalyticsTarget.ROOT, StakingMode.ADD_STAKE, UnstakeMode.FULL,
            BittensorTransactionFailureReason.NOT_SUBMITTED, StakingFailureStage.NOT_SUBMITTED, true
        )
        assertEquals("full", unstake.properties["mode"])
        assertEquals("add_stake", stake.properties["mode"])
    }

    @Test
    fun `count buckets retain boundaries`() {
        mapOf(-1 to "0", 0 to "0", 1 to "1", 2 to "2", 3 to "3_plus", 10 to "3_plus").forEach { (count, bucket) ->
            val event = BittensorAnalyticsEvent.ClaimInitiated("Bittensor", null, count)
            assertEquals(bucket, event.properties["validators_count_bucket"])
        }
    }
}
