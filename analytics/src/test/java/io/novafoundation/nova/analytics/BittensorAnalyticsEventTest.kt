package io.novafoundation.nova.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BittensorAnalyticsEventTest {

    @Test
    fun `selection retains wire name and string netuid`() {
        val event = BittensorAnalyticsEvent.TypeSelected(
            network = "Bittensor",
            StakingAnalyticsTarget.SUBNET,
            StakingAnalyticsSelectionMethod.PICKER_SUBNET,
            netuid = 42
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
            network = "Bittensor",
            StakingAnalyticsTarget.SUBNET,
            StakingMode.BUY_MORE,
            StakingValidatorSource.MANUAL,
            slippage = null,
            amount = null
        )
        val withOptionals = BittensorAnalyticsEvent.Initiated(
            network = "Bittensor",
            StakingAnalyticsTarget.SUBNET,
            StakingMode.BUY_MORE,
            StakingValidatorSource.MANUAL,
            SlippageBucket.LOW,
            AmountBucket.UNDER_1
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
        val unknown = BittensorAnalyticsEvent.ClaimCompleted(
            network = "Bittensor",
            amount = null,
            DurationBucket.UNDER_5S,
            claimedZero = null
        )
        val nonZero = BittensorAnalyticsEvent.ClaimCompleted(
            network = "Bittensor",
            amount = null,
            DurationBucket.UNDER_5S,
            claimedZero = false
        )

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
                flow,
                network = "Bittensor",
                StakingAnalyticsTarget.ROOT,
                mode = null,
                unstakeMode = null,
                BittensorTransactionFailureReason.NOT_SUBMITTED,
                StakingFailureStage.NOT_SUBMITTED,
                retryable = false
            )
            assertEquals(name, event.name)
            assertFalse(event.properties.containsKey("mode"))
            assertEquals(false, event.properties["retryable"])
        }

        val unstake = BittensorAnalyticsEvent.Failed(
            StakingFlow.UNSTAKE,
            network = "Bittensor",
            StakingAnalyticsTarget.ROOT,
            mode = null,
            UnstakeMode.FULL,
            BittensorTransactionFailureReason.NOT_SUBMITTED,
            StakingFailureStage.NOT_SUBMITTED,
            retryable = true
        )
        val stake = BittensorAnalyticsEvent.Failed(
            StakingFlow.STAKE,
            network = "Bittensor",
            StakingAnalyticsTarget.ROOT,
            StakingMode.ADD_STAKE,
            UnstakeMode.FULL,
            BittensorTransactionFailureReason.NOT_SUBMITTED,
            StakingFailureStage.NOT_SUBMITTED,
            retryable = true
        )
        assertEquals("full", unstake.properties["mode"])
        assertEquals("add_stake", stake.properties["mode"])
    }

    @Test
    fun `count buckets retain boundaries`() {
        mapOf(-1 to "0", 0 to "0", 1 to "1", 2 to "2", 3 to "3_plus", 10 to "3_plus").forEach { (count, bucket) ->
            val event = BittensorAnalyticsEvent.ClaimInitiated(network = "Bittensor", amount = null, count)
            assertEquals(bucket, event.properties["validators_count_bucket"])
        }
    }

    @Test
    fun `subnet interactions retain wire name and action properties`() {
        val search = BittensorAnalyticsEvent.SubnetSearch()
        val sort = BittensorAnalyticsEvent.SubnetSort(SubnetSortKey.SEVEN_DAY_CHANGE)
        val favoriteAdded = BittensorAnalyticsEvent.SubnetFavorite(added = true)
        val favoriteRemoved = BittensorAnalyticsEvent.SubnetFavorite(added = false)

        listOf(search, sort, favoriteAdded, favoriteRemoved).forEach { event ->
            assertEquals("subnet_list_interacted", event.name)
        }
        assertEquals(mapOf("action" to "search"), search.properties)
        assertEquals(mapOf("action" to "sort", "value" to "seven_day_change"), sort.properties)
        assertEquals(mapOf("action" to "favorite_added"), favoriteAdded.properties)
        assertEquals(mapOf("action" to "favorite_removed"), favoriteRemoved.properties)
    }

    @Test
    fun `subnet filter retains boolean values including false`() {
        listOf(false, true).forEach { enabled ->
            val event = BittensorAnalyticsEvent.SubnetFilter(enabled)

            assertEquals("subnet_list_interacted", event.name)
            assertEquals(mapOf("action" to "filter_thin_pools", "value" to enabled), event.properties)
        }
    }
}
