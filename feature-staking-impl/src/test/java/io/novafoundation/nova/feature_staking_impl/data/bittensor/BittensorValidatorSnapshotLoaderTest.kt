package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.ComponentHolder
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.runtime.storage.source.query.DynamicInstanceBinder
import io.novafoundation.nova.runtime.storage.source.query.DynamicInstanceBinderWithKey
import io.novafoundation.nova.runtime.storage.source.query.StorageKeyComponents
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.StorageEntryModifier
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import io.novasama.substrate_sdk_android.runtime.metadata.module.Storage
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntry
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntryType
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BittensorValidatorSnapshotLoaderTest {

    private val validator = BittensorValidator(AccountIdKey(ByteArray(32) { 1 }), "Validator")

    private val delegate = mock(StorageQueryContext::class.java)

    private val runtime = createRuntime()

    @Test
    fun `parent and child links are batched deduplicated and scoped to subnet`() = runBlocking {
        val first = BittensorValidatorLink(validator.hotkey, 1)
        val second = BittensorValidatorLink(validator.hotkey, 64)
        val absent = BittensorValidatorLink(validator.hotkey, 65)
        val context = RecordingQueryContext(runtime, delegate, expectedReads = 2, links = mapOf(
            "ParentKeys" to setOf(first),
            "ChildKeys" to setOf(first, second)
        ))

        val result = withTimeout(5_000) { context.loadBittensorValidatorLinks(listOf(first, first, second, absent)) }

        assertEquals(setOf(first, second), result)
        assertEquals(mapOf("ParentKeys" to 3, "ChildKeys" to 3), context.batchSizes)
    }

    @Test
    fun `all independent storage reads start before any result is awaited`() = runBlocking {
        val context = RecordingQueryContext(runtime, delegate, expectedReads = 9)

        val snapshot = withTimeout(5_000) {
            context.loadBittensorValidatorSnapshot(BittensorNetuid.Subnet(1), listOf(validator))
        }

        assertEquals(setOf("ValidatorPermit", "LastUpdate", "Number", "Tempo", "ActivityCutoffFactorMilli", "Uids", "Delegates", "ParentKeys", "ChildKeys"), context.requests)
        assertEquals(mapOf(validator.hotkey to 0), snapshot.uids)
        assertEquals(mapOf(validator.hotkey to BigInteger.ZERO), snapshot.commissionsInU16)
        assertEquals(listOf(true), snapshot.permits)
        assertEquals(listOf(100.toBigInteger()), snapshot.lastUpdates)
        assertEquals(100.toBigInteger(), snapshot.activeThreshold)
    }

    @Test
    fun `directory snapshot includes the subnet registration block`() = runBlocking {
        val context = RecordingQueryContext(runtime, delegate, expectedReads = 10)

        val snapshot = withTimeout(5_000) {
            context.loadBittensorValidatorSnapshot(BittensorNetuid.Subnet(1), listOf(validator), includeRegistration = true)
        }

        assertEquals(90.toBigInteger(), snapshot.registeredAt)
        assertTrue("NetworkRegisteredAt" in context.requests)
    }

    @Test
    fun `root only loads registration and take`() = runBlocking {
        val context = RecordingQueryContext(runtime, delegate, expectedReads = 2)

        val snapshot = withTimeout(5_000) {
            context.loadBittensorValidatorSnapshot(BittensorNetuid.Root, listOf(validator))
        }

        assertEquals(setOf("Uids", "Delegates"), context.requests)
        assertTrue(snapshot.permits.isEmpty())
        assertTrue(snapshot.lastUpdates.isEmpty())
        assertEquals(BigInteger.ZERO, snapshot.activeThreshold)
    }

    @Test
    fun `failed storage read cancels other reads instead of returning a partial snapshot`() = runBlocking {
        val failure = IllegalStateException("Storage unavailable")
        val context = RecordingQueryContext(runtime, delegate, expectedReads = 9, failure = failure)

        try {
            withTimeout(5_000) {
                context.loadBittensorValidatorSnapshot(BittensorNetuid.Subnet(1), listOf(validator))
            }
            fail("Expected storage failure")
        } catch (exception: IllegalStateException) {
            assertEquals(failure.message, exception.message)
        }

        assertTrue(context.cancelledReads > 0)
    }

    private fun createRuntime(): RuntimeSnapshot {
        val subtensorModule = createModule(
            "SubtensorModule",
            listOf("ValidatorPermit", "LastUpdate", "Tempo", "ActivityCutoffFactorMilli", "Uids", "Delegates", "NetworkRegisteredAt", "ParentKeys", "ChildKeys")
        )
        val systemModule = createModule("System", listOf("Number"))
        val modules = mapOf(
            "SubtensorModule" to subtensorModule,
            "System" to systemModule
        )
        val metadata = mock(RuntimeMetadata::class.java)
        `when`(metadata.modules).thenReturn(modules)
        val runtime = mock(RuntimeSnapshot::class.java)
        `when`(runtime.metadata).thenReturn(metadata)
        return runtime
    }

    private fun createModule(name: String, entries: List<String>): Module {
        val storageEntries = entries.associateWith { entry ->
            val type = StorageEntryType.Plain(null)
            StorageEntry(entry, StorageEntryModifier.Default, type, byteArrayOf(), emptyList(), name)
        }
        val storage = Storage(name, storageEntries)
        return Module(name, storage, null, null, emptyMap(), emptyMap(), BigInteger.ZERO)
    }

    private class RecordingQueryContext(
        override val runtime: RuntimeSnapshot,
        delegate: StorageQueryContext,
        private val expectedReads: Int,
        private val failure: Exception? = null,
        private val links: Map<String, Set<BittensorValidatorLink>> = emptyMap()
    ) : StorageQueryContext by delegate {

        val requests = mutableSetOf<String>()

        val batchSizes = mutableMapOf<String, Int>()

        var cancelledReads = 0
            private set

        private val allStarted = CompletableDeferred<Unit>()

        override suspend fun <V> StorageEntry.query(vararg keyArguments: Any?, binding: DynamicInstanceBinder<V>): V {
            record(name)
            val value: Any = when (name) {
                "ValidatorPermit" -> listOf(true)
                "LastUpdate" -> listOf(100.toBigInteger())
                "Number" -> 110.toBigInteger()
                "Tempo" -> BigInteger.TEN
                "ActivityCutoffFactorMilli" -> 1000.toBigInteger()
                "NetworkRegisteredAt" -> 90.toBigInteger()
                else -> error("Unexpected storage $name")
            }
            return binding(value)
        }

        override suspend fun <K, V> StorageEntry.entries(
            keysArguments: List<List<Any?>>,
            keyExtractor: (StorageKeyComponents) -> K,
            binding: DynamicInstanceBinderWithKey<K, V>,
            recover: (exception: Exception, rawValue: String?) -> Unit
        ): Map<K, V> {
            batchSizes[name] = keysArguments.size
            record(name)
            return keysArguments.associate { arguments ->
                val components = ComponentHolder(arguments)
                val key = keyExtractor(components)
                val value = if (name == "ParentKeys" || name == "ChildKeys") {
                    if (key is BittensorValidatorLink && key in links[name].orEmpty()) {
                        listOf(listOf(BigInteger.ONE, ByteArray(32)))
                    } else {
                        emptyList<Any>()
                    }
                } else {
                    BigInteger.ZERO
                }
                key to binding(value, key)
            }
        }

        private suspend fun record(name: String) {
            requests += name
            if (name == "Uids" && failure != null) throw failure
            if (requests.size == expectedReads) allStarted.complete(Unit)
            try {
                allStarted.await()
            } catch (exception: CancellationException) {
                cancelledReads++
                throw exception
            }
        }
    }
}
