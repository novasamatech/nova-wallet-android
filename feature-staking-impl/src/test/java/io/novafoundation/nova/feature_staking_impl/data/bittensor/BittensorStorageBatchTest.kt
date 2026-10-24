package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.BooleanType
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.u16
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.u64
import io.novasama.substrate_sdk_android.runtime.metadata.StorageEntryModifier
import io.novasama.substrate_sdk_android.runtime.metadata.StorageHasher
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntry
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntryType
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock

class BittensorStorageBatchTest {

    private val runtime = mock(RuntimeSnapshot::class.java)

    private val factor = StorageEntry(
        "NominatorMinRequiredStake",
        StorageEntryModifier.Default,
        StorageEntryType.Plain(u64),
        ByteArray(8),
        emptyList(),
        "SubtensorModule"
    )

    private val networksAdded = StorageEntry(
        "NetworksAdded",
        StorageEntryModifier.Default,
        StorageEntryType.NMap(listOf(u16), listOf(StorageHasher.Identity), BooleanType),
        byteArrayOf(0),
        emptyList(),
        "SubtensorModule"
    )

    @Test
    fun `plain and keyed items are read in one fetch and matched by full key`() = runBlocking {
        val batch = BittensorStorageBatch(runtime)
        val minimum = batch.add(factor) { it?.let(::bindNumber) }
        val subnet12 = batch.add(networksAdded, BigInteger.valueOf(12)) { it as Boolean? }
        val subnet64 = batch.add(networksAdded, BigInteger.valueOf(64)) { it as Boolean? }
        val requests = mutableListOf<List<String>>()

        batch.execute { keys ->
            requests += keys
            mapOf(
                factor.storageKey(runtime) to "0x8096980000000000",
                networksAdded.storageKey(runtime, BigInteger.valueOf(12)) to "0x01",
                networksAdded.storageKey(runtime, BigInteger.valueOf(64)) to "0x00"
            )
        }

        assertEquals(1, requests.size)
        assertEquals(3, requests.single().size)
        assertEquals(BigInteger.valueOf(10_000_000), minimum.value)
        assertEquals(true, subnet12.value)
        assertEquals(false, subnet64.value)
    }

    @Test
    fun `absent key binds null while a stored zero decodes as zero`() = runBlocking {
        val batch = BittensorStorageBatch(runtime)
        val absent = batch.add(factor) { it?.let(::bindNumber) }
        val zeroBatch = BittensorStorageBatch(runtime)
        val zero = zeroBatch.add(factor) { it?.let(::bindNumber) }

        batch.execute { keys -> keys.associateWith { null } }
        zeroBatch.execute { keys -> keys.associateWith { "0x0000000000000000" } }

        assertNull(absent.value)
        assertEquals(BigInteger.ZERO, zero.value)
    }

    @Test
    fun `duplicate items share one key and an empty batch makes no request`() = runBlocking {
        val batch = BittensorStorageBatch(runtime)
        batch.add(factor) { it }
        batch.add(factor) { it }
        var requested = emptyList<String>()
        batch.execute { keys -> requested = keys; emptyMap() }
        assertEquals(1, requested.size)

        var called = false
        BittensorStorageBatch(runtime).execute { called = true; emptyMap() }
        assertEquals(false, called)
    }
}
