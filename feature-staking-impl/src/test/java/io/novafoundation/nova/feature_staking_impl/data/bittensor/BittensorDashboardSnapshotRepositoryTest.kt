package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.Gson
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorDashboardSnapshotRepositoryTest {

    private val preferences = InMemoryPreferences()

    private val repository = RealBittensorDashboardSnapshotRepository(preferences, Gson())

    private val coldKey = ByteArray(32) { 1 }

    private val otherColdKey = ByteArray(32) { 2 }

    private val snapshot = BittensorDashboardSnapshot(
        totalStakeInPlanks = BigInteger("19990000"),
        rootStakeInPlanks = BigInteger("19990000"),
        subnetCount = 0,
        rootAnnualPercentageYield = BigDecimal("0.123456789012"),
        subnetAnnualPercentageYield = null,
        maximumAnnualPercentageYield = BigDecimal("0.654321")
    )

    @Test
    fun `snapshot round trips per account without exposing the account id`() {
        repository.saveSnapshot("chain", coldKey, snapshot)

        assertEquals(snapshot, repository.snapshot("chain", coldKey))
        assertNull(repository.snapshot("chain", otherColdKey))
        assertNull(repository.snapshot("other", coldKey))
        assertTrue(preferences.values.keys.none { it.contains("01".repeat(32)) })
    }

    @Test
    fun `corrupted snapshot reads as absent`() {
        repository.saveSnapshot("chain", coldKey, snapshot)
        val key = preferences.values.keys.single()
        preferences.putString(key, "{not json")

        assertNull(repository.snapshot("chain", coldKey))
    }

    @Test
    fun `claimable rewards keep only positive amounts and notify observers`() = runBlocking {
        val updates = async { repository.claimableRootRewardsFlow("chain", coldKey).take(3).toList() }
        yield()
        repository.saveClaimableRootRewards("chain", coldKey, BigInteger.TEN)
        yield()
        repository.saveClaimableRootRewards("chain", coldKey, BigInteger.ZERO)

        assertEquals(listOf(null, BigInteger.TEN, null), updates.await())
        assertNull(repository.claimableRootRewards("chain", coldKey))
        assertTrue(preferences.values.isEmpty())
    }
}
