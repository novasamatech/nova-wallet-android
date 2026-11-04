package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.Gson
import io.novafoundation.nova.core_db.dao.ResponseCacheDao
import io.novafoundation.nova.core_db.model.ResponseCacheLocal
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.io.IOException
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorDashboardSnapshotSourceTest {

    private val owners = mutableSetOf(1L, 2L)

    private val dao = InMemoryResponseCacheDao(owners)

    private val cache = createResponseTestCache(dao = dao)

    private val preferences = InMemoryPreferences()

    private val gson = Gson()

    private val logger = mock(Logger::class.java)

    private val source = BittensorDashboardSnapshotSource(cache, preferences, gson, logger)

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
    fun `snapshot survives restart and is isolated by wallet chain and account`() = runTest {
        source.saveSnapshot("chain", coldKey, 1L, snapshot)
        val restartedCache = createResponseTestCache(dao = dao)
        val restarted = BittensorDashboardSnapshotSource(restartedCache, preferences, gson, logger)

        assertEquals(snapshot, restarted.snapshot("chain", coldKey, 1L))
        assertNull(restarted.snapshot("chain", coldKey, 2L))
        assertNull(restarted.snapshot("chain", otherColdKey, 1L))
        assertNull(restarted.snapshot("other", coldKey, 1L))
        assertTrue(dao.rows.keys.none { it.contains(coldKey.toHexString()) })
        assertTrue(preferences.values.isEmpty())
    }

    @Test
    fun `corrupted snapshot reads as absent`() = runTest {
        source.saveSnapshot("chain", coldKey, 1L, snapshot)
        val row = dao.rows.values.single()
        dao.put(row.copy(payload = "{not json"))

        assertNull(source.snapshot("chain", coldKey, 1L))
    }

    @Test
    fun `claimable rewards keep only positive amounts and notify observers`() = runTest {
        val updates = async { source.claimableRootRewardsFlow("chain", coldKey, 1L).take(3).toList() }
        runCurrent()
        source.saveClaimableRootRewards("chain", coldKey, 1L, BigInteger.TEN)
        runCurrent()
        source.saveClaimableRootRewards("chain", coldKey, 1L, BigInteger.ZERO)

        assertEquals(listOf(null, BigInteger.TEN, null), updates.await())
        assertNull(source.claimableRootRewards("chain", coldKey, 1L))
        assertTrue(preferences.values.isEmpty())
    }

    @Test
    fun `deleting wallet clears its dashboard and observed rewards but keeps another wallet`() = runTest {
        source.saveSnapshot("chain", coldKey, 1L, snapshot)
        source.saveSnapshot("chain", coldKey, 2L, snapshot)
        source.saveClaimableRootRewards("chain", coldKey, 1L, BigInteger.TEN)
        val updates = async { source.claimableRootRewardsFlow("chain", coldKey, 1L).take(2).toList() }
        runCurrent()

        dao.deleteOwner(1L)

        assertEquals(listOf(BigInteger.TEN, null), updates.await())
        assertNull(source.snapshot("chain", coldKey, 1L))
        assertEquals(snapshot, source.snapshot("chain", coldKey, 2L))
        source.saveSnapshot("chain", coldKey, 1L, snapshot)
        assertTrue(dao.rows.values.none { it.metaAccountId == 1L })
        verify(logger).log("[BittensorDashboard] Snapshot could not be saved")
    }

    @Test
    fun `legacy dashboard and rewards migrate once to the explicit owner`() = runTest {
        val identity = ("chain".toByteArray() + coldKey).blake2b256().toHexString()
        preferences.putString(
            "bittensor_dashboard_snapshot_$identity",
            """{"total":"19990000","root":"19990000","subnets":0,"rootRate":"0.123456789012","maxRate":"0.654321"}"""
        )
        preferences.putString("bittensor_dashboard_claimable_$identity", "10")

        assertEquals(snapshot, source.snapshot("chain", coldKey, 1L))
        assertEquals(BigInteger.TEN, source.claimableRootRewards("chain", coldKey, 1L))
        assertTrue(preferences.values.isEmpty())
        assertTrue(dao.rows.values.all { it.metaAccountId == 1L })
        assertNull(source.snapshot("chain", coldKey, 2L))
        assertNull(source.claimableRootRewards("chain", coldKey, 2L))
    }

    @Test
    fun `a fresh snapshot waits for concurrent legacy migration and replaces it`() = runTest {
        putLegacySnapshot()
        val gatedDao = FirstReadGateDao(dao)
        val gatedCache = createResponseTestCache(dao = gatedDao, scope = backgroundScope)
        val migratingSource = BittensorDashboardSnapshotSource(gatedCache, preferences, gson, logger)
        val migration = async { migratingSource.snapshot("chain", coldKey, 1L) }
        gatedDao.readStarted.await()
        val fresh = snapshot.copy(totalStakeInPlanks = BigInteger("30000000"))
        val save = async { migratingSource.saveSnapshot("chain", coldKey, 1L, fresh) }
        runCurrent()
        assertFalse(save.isCompleted)

        gatedDao.releaseRead.complete(Unit)
        assertEquals(snapshot, migration.await())
        save.await()

        assertEquals(fresh, migratingSource.snapshot("chain", coldKey, 1L))
        assertTrue(preferences.values.isEmpty())
    }

    @Test
    fun `clearing rewards waits for concurrent legacy migration and does not resurrect them`() = runTest {
        preferences.putString(legacyKey("claimable"), "10")
        val gatedDao = FirstReadGateDao(dao)
        val gatedCache = createResponseTestCache(dao = gatedDao, scope = backgroundScope)
        val migratingSource = BittensorDashboardSnapshotSource(gatedCache, preferences, gson, logger)
        val migration = async { migratingSource.claimableRootRewards("chain", coldKey, 1L) }
        gatedDao.readStarted.await()
        val clear = async { migratingSource.saveClaimableRootRewards("chain", coldKey, 1L, BigInteger.ZERO) }
        runCurrent()
        assertFalse(clear.isCompleted)

        gatedDao.releaseRead.complete(Unit)
        assertEquals(BigInteger.TEN, migration.await())
        clear.await()

        assertNull(migratingSource.claimableRootRewards("chain", coldKey, 1L))
        assertNull(migratingSource.claimableRootRewardsFlow("chain", coldKey, 1L).first())
        assertTrue(preferences.values.isEmpty())
    }

    @Test
    fun `failed Room migration keeps legacy rewards visible during observation and after recreation`() = runTest {
        val legacyKey = legacyKey("claimable")
        preferences.putString(legacyKey, "10")
        val failingDao = object : ResponseCacheDao by dao {
            override suspend fun put(entry: ResponseCacheLocal) = throw IOException("disk unavailable")
        }
        val failedCache = createResponseTestCache(dao = failingDao, scope = backgroundScope)
        val observingSource = BittensorDashboardSnapshotSource(failedCache, preferences, gson, logger)
        val updates = mutableListOf<BigInteger?>()
        val observation = backgroundScope.launch {
            observingSource.claimableRootRewardsFlow("chain", coldKey, 1L).toList(updates)
        }
        runCurrent()
        assertEquals(listOf(BigInteger.TEN), updates)
        assertEquals("10", preferences.getString(legacyKey))
        assertTrue(dao.rows.isEmpty())

        val restartedCache = createResponseTestCache(dao = failingDao, scope = backgroundScope)
        val restarted = BittensorDashboardSnapshotSource(restartedCache, preferences, gson, logger)
        assertEquals(BigInteger.TEN, restarted.claimableRootRewardsFlow("chain", coldKey, 1L).first())
        assertEquals("10", preferences.getString(legacyKey))
        observation.cancelAndJoin()
    }

    private fun putLegacySnapshot() {
        preferences.putString(
            legacyKey("snapshot"),
            """{"total":"19990000","root":"19990000","subnets":0,"rootRate":"0.123456789012","maxRate":"0.654321"}"""
        )
    }

    private fun legacyKey(resource: String): String {
        val identity = ("chain".toByteArray() + coldKey).blake2b256().toHexString()
        return "bittensor_dashboard_${resource}_$identity"
    }

    private class FirstReadGateDao(private val delegate: ResponseCacheDao) : ResponseCacheDao by delegate {

        val readStarted = CompletableDeferred<Unit>()

        val releaseRead = CompletableDeferred<Unit>()

        private var firstRead = true

        override suspend fun get(url: String, metaAccountId: Long?): ResponseCacheLocal? {
            val value = delegate.get(url, metaAccountId)
            if (firstRead) {
                firstRead = false
                readStarted.complete(Unit)
                releaseRead.await()
            }
            return value
        }
    }

    @Test
    fun `dashboard snapshots share the response cache storage limit`() = runTest {
        repeat(513) { source.saveSnapshot("chain$it", coldKey, 1L, snapshot) }

        assertEquals(512, dao.rows.size)
        assertTrue(preferences.values.isEmpty())
    }
}
