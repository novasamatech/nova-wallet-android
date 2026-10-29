package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.Gson
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorDashboardSnapshotRepositoryTest {

    private val storedPreferences = InMemoryPreferences()

    private val preferences = spy(storedPreferences)

    private val gson = Gson()

    private val repository = RealBittensorDashboardSnapshotRepository(preferences, gson)

    private val chainId = "chain"

    private val otherChainId = "other"

    private val coldKey = ByteArray(size = 32) { 1 }

    private val otherColdKey = ByteArray(size = 32) { 2 }

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
        repository.saveSnapshot(chainId, coldKey, snapshot)

        assertEquals(snapshot, repository.snapshot(chainId, coldKey))
        assertNull(repository.snapshot(chainId, otherColdKey))
        assertNull(repository.snapshot(otherChainId, coldKey))
        assertTrue(preferences.values.keys.none { it.contains("01".repeat(32)) || it.contains(chainId) })
    }

    @Test
    fun `corrupted snapshot reads as absent`() {
        repository.saveSnapshot(chainId, coldKey, snapshot)
        val key = preferences.values.keys.single()

        listOf("{not json", "null", "{}", "[]", "{\"subnets\":0,\"total\":\"invalid\"}", "{\"subnets\":0,\"rootRate\":\"invalid\"}")
            .forEach { raw ->
                preferences.putString(key, raw)
                assertNull(repository.snapshot(chainId, coldKey))
            }
    }

    @Test
    fun `claimable rewards keep only positive amounts and notify observers`() = runTest {
        val updates = mutableListOf<BigInteger?>()
        val observer = backgroundScope.launch { repository.claimableRootRewardsFlow(chainId, coldKey).collect { updates += it } }
        runCurrent()
        repository.saveClaimableRootRewards(chainId, coldKey, BigInteger.TEN)
        runCurrent()
        repository.saveClaimableRootRewards(chainId, coldKey, BigInteger.ZERO)
        runCurrent()

        assertEquals(listOf(null, BigInteger.TEN, null), updates)
        assertNull(repository.claimableRootRewards(chainId, coldKey))
        assertTrue(preferences.values.isEmpty())
        observer.cancelAndJoin()
    }

    @Test
    fun `new repository instance restores exact large amounts and decimal scales`() {
        val precise = snapshot.copy(
            totalStakeInPlanks = BigInteger("1234567890123456789012345678901234567890"),
            rootAnnualPercentageYield = BigDecimal("0.000000000000000000123456789000"),
            subnetAnnualPercentageYield = BigDecimal("12345678901234567890.012345678900")
        )
        repository.saveSnapshot(chainId, coldKey, precise)
        repository.saveClaimableRootRewards(chainId, coldKey, precise.totalStakeInPlanks)
        val restoredRepository = RealBittensorDashboardSnapshotRepository(preferences, gson)

        assertEquals(precise, restoredRepository.snapshot(chainId, coldKey))
        assertEquals(precise.totalStakeInPlanks, restoredRepository.claimableRootRewards(chainId, coldKey))
    }

    @Test
    fun `known zero fields remain distinct from absent persisted fields`() {
        val zero = BittensorDashboardSnapshot(
            totalStakeInPlanks = BigInteger.ZERO,
            rootStakeInPlanks = BigInteger.ZERO,
            subnetCount = 0,
            rootAnnualPercentageYield = BigDecimal.ZERO,
            subnetAnnualPercentageYield = BigDecimal.ZERO,
            maximumAnnualPercentageYield = BigDecimal.ZERO
        )
        val absent = BittensorDashboardSnapshot(
            totalStakeInPlanks = null,
            rootStakeInPlanks = null,
            subnetCount = 0,
            rootAnnualPercentageYield = null,
            subnetAnnualPercentageYield = null,
            maximumAnnualPercentageYield = null
        )

        repository.saveSnapshot(chainId, coldKey, zero)
        repository.saveSnapshot(chainId, otherColdKey, absent)

        assertEquals(zero, repository.snapshot(chainId, coldKey))
        assertEquals(absent, repository.snapshot(chainId, otherColdKey))
    }

    @Test
    fun `snapshot and claimable rewards have independent keys and writes`() {
        repository.saveSnapshot(chainId, coldKey, snapshot)
        repository.saveClaimableRootRewards(chainId, coldKey, BigInteger.TEN)
        assertEquals(2, preferences.values.size)

        val updatedSnapshot = snapshot.copy(subnetCount = 2)
        repository.saveSnapshot(chainId, coldKey, updatedSnapshot)
        assertEquals(BigInteger.TEN, repository.claimableRootRewards(chainId, coldKey))

        repository.saveClaimableRootRewards(chainId, coldKey, amount = null)
        assertEquals(updatedSnapshot, repository.snapshot(chainId, coldKey))
        assertEquals(1, preferences.values.size)
    }

    @Test
    fun `claimable rewards remain isolated by both account and chain`() {
        repository.saveClaimableRootRewards(chainId, coldKey, BigInteger.ONE)
        repository.saveClaimableRootRewards(chainId, otherColdKey, BigInteger.TEN)
        repository.saveClaimableRootRewards(otherChainId, coldKey, BigInteger.TWO)

        assertEquals(BigInteger.ONE, repository.claimableRootRewards(chainId, coldKey))
        assertEquals(BigInteger.TEN, repository.claimableRootRewards(chainId, otherColdKey))
        assertEquals(BigInteger.TWO, repository.claimableRootRewards(otherChainId, coldKey))
        assertTrue(preferences.values.keys.none { it.contains("01".repeat(32)) || it.contains("02".repeat(32)) })
    }

    @Test
    fun `nonpositive or absent amounts clear rewards`() {
        listOf(null, BigInteger.ZERO, BigInteger.ONE.negate()).forEach { amount ->
            repository.saveClaimableRootRewards(chainId, coldKey, BigInteger.TEN)
            repository.saveClaimableRootRewards(chainId, coldKey, amount)
            assertNull(repository.claimableRootRewards(chainId, coldKey))
            assertTrue(preferences.values.isEmpty())
        }
    }

    @Test
    fun `unchanged amounts and repeated clears do not write preferences again`() {
        repository.saveClaimableRootRewards(chainId, coldKey, BigInteger.TEN)
        val key = preferences.values.keys.single()
        repository.saveClaimableRootRewards(chainId, coldKey, BigInteger.TEN)
        repository.saveClaimableRootRewards(chainId, coldKey, BigInteger.ZERO)
        repository.saveClaimableRootRewards(chainId, coldKey, amount = null)
        repository.saveClaimableRootRewards(chainId, coldKey, BigInteger.ONE.negate())

        verify(preferences, times(1)).putString(key, BigInteger.TEN.toString())
        verify(preferences, times(1)).removeField(key)
    }

    @Test
    fun `malformed persisted rewards are absent but exact positive integers are retained`() {
        repository.saveClaimableRootRewards(chainId, coldKey, BigInteger.ONE)
        val key = preferences.values.keys.single()
        listOf("", "invalid", "0", "-1", "1.5").forEach { raw ->
            preferences.putString(key, raw)
            assertNull(repository.claimableRootRewards(chainId, coldKey))
        }
        val large = BigInteger("99999999999999999999999999999999999999999999999999")
        preferences.putString(key, large.toString())
        assertEquals(large, repository.claimableRootRewards(chainId, coldKey))
    }

    @Test
    fun `observers emit the stored amount and ignore unrelated writes and repeated values`() = runTest {
        repository.saveClaimableRootRewards(chainId, coldKey, BigInteger.TEN)
        val claimableKey = preferences.values.keys.single()
        val updates = mutableListOf<BigInteger?>()
        val observer = backgroundScope.launch { repository.claimableRootRewardsFlow(chainId, coldKey).collect { updates += it } }
        runCurrent()

        repository.saveSnapshot(chainId, coldKey, snapshot)
        repository.saveClaimableRootRewards(otherChainId, coldKey, BigInteger.ONE)
        repository.saveClaimableRootRewards(chainId, otherColdKey, BigInteger.ONE)
        repository.saveClaimableRootRewards(chainId, coldKey, BigInteger.TEN)
        runCurrent()
        assertEquals(listOf(BigInteger.TEN), updates)

        preferences.putString(claimableKey, value = "010")
        runCurrent()
        assertEquals(listOf(BigInteger.TEN), updates)

        repository.saveClaimableRootRewards(chainId, coldKey, BigInteger.ONE)
        runCurrent()
        assertEquals(listOf(BigInteger.TEN, BigInteger.ONE), updates)

        observer.cancelAndJoin()
        repository.saveClaimableRootRewards(chainId, coldKey, BigInteger.TWO)
        runCurrent()
        assertEquals(listOf(BigInteger.TEN, BigInteger.ONE), updates)
        assertEquals(BigInteger.TWO, repository.claimableRootRewards(chainId, coldKey))
    }
}
