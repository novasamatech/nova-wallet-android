package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.storage.Editor
import io.novafoundation.nova.common.data.storage.InitialValueProducer
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.core.model.Language
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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

    private val repository = RealBittensorDashboardSnapshotRepository(preferences)

    private val coldKey = ByteArray(32) { 1 }

    private val otherColdKey = ByteArray(32) { 2 }

    private val snapshot = BittensorDashboardSnapshot(
        totalStakeInPlanks = BigInteger("19990000"),
        rootStakeInPlanks = BigInteger("19990000"),
        subnetCount = 0,
        rootAnnualPercentageYield = BigDecimal("0.123456789012"),
        subnetAnnualPercentageYield = null
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

    class InMemoryPreferences : Preferences {

        val values = mutableMapOf<String, String>()

        private val changes = MutableSharedFlow<String>(extraBufferCapacity = 16)

        override fun contains(field: String) = field in values

        override fun putString(field: String, value: String?) {
            if (value == null) values.remove(field) else values[field] = value
            changes.tryEmit(field)
        }

        override fun getString(field: String, defaultValue: String) = values[field] ?: defaultValue

        override fun getString(field: String): String? = values[field]

        override fun removeField(field: String) = putString(field, null)

        override fun keyFlow(key: String): Flow<String> = changes.filter { it == key }.onStart { emit(key) }

        override fun keysFlow(vararg keys: String): Flow<List<String>> = changes.filter { it in keys }.map { listOf(it) }

        override fun putBoolean(field: String, value: Boolean) = unsupported()

        override fun getBoolean(field: String, defaultValue: Boolean): Boolean = unsupported()

        override fun putInt(field: String, value: Int) = unsupported()

        override fun putStringSet(field: String, value: Set<String>?) = unsupported()

        override fun getInt(field: String, defaultValue: Int): Int = unsupported()

        override fun putLong(field: String, value: Long) = unsupported()

        override fun getLong(field: String, defaultValue: Long): Long = unsupported()

        override fun getStringSet(field: String): Set<String> = unsupported()

        override fun getCurrentLanguage(): Language? = unsupported()

        override fun saveCurrentLanguage(languageIsoCode: String) = unsupported()

        override fun stringFlow(field: String, initialValueProducer: InitialValueProducer<String>?): Flow<String?> = unsupported()

        override fun booleanFlow(field: String, defaultValue: Boolean): Flow<Boolean> = unsupported()

        override fun stringSetFlow(field: String, initialValueProducer: InitialValueProducer<Set<String>>?): Flow<Set<String>?> = unsupported()

        override fun edit(): Editor = unsupported()

        private fun unsupported(): Nothing = throw UnsupportedOperationException()
    }
}
