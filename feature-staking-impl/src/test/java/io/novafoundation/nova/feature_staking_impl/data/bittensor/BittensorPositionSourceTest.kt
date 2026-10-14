package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.FixedByteArray
import io.novasama.substrate_sdk_android.runtime.metadata.encodeInputs
import io.novasama.substrate_sdk_android.runtime.metadata.module.RuntimeApiMethod
import io.novasama.substrate_sdk_android.runtime.metadata.module.RuntimeApiMethodParam
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import java.math.BigInteger

class BittensorPositionSourceTest {

    @Test
    fun `root and subnet requests encode the coldkey_account expected by runtime metadata`() = runBlocking {
        val coldKey: AccountId = ByteArray(32) { it.toByte() }
        val calls = PositionRuntimeCalls()
        val source = RealBittensorPositionSource(object : MultiChainRuntimeCallsApi {
            override suspend fun forChain(chainId: String): RuntimeCallsApi {
                assertEquals("bittensor", chainId)
                return calls
            }

            override suspend fun isSupported(chainId: String, section: String, method: String) = true
        })

        assertEquals(BigInteger.TEN, source.rootPositions("bittensor", coldKey).single().stakeInPlanks)
        assertEquals(BigInteger.TEN, source.subnetPositions("bittensor", coldKey).single().alphaInPlanks)
        assertEquals(2, calls.encodedInputs.size)
        assertTrue(calls.encodedInputs.all { it == coldKey.toHexString(withPrefix = true) })
    }

    private class PositionRuntimeCalls : RuntimeCallsApi {

        override val chainId = "bittensor"
        override val runtime = mock(RuntimeSnapshot::class.java)
        val encodedInputs = mutableListOf<String>()

        override fun isSupported(section: String, method: String) = true

        override suspend fun <R> call(
            section: String,
            method: String,
            arguments: Map<String, Any?>,
            returnBinding: (Any?) -> R
        ): R {
            assertEquals("StakeInfoRuntimeApi", section)
            assertEquals("get_stake_info_for_coldkey", method)
            val metadata = RuntimeApiMethod(
                section,
                method,
                listOf(RuntimeApiMethodParam("coldkey_account", FixedByteArray("AccountId32", 32))),
                null
            )
            encodedInputs += metadata.encodeInputs(runtime, arguments)
            return returnBinding(listOf(position(0), position(64)))
        }

        override suspend fun <R> call(
            section: String,
            method: String,
            arguments: List<Pair<Any?, String?>>,
            returnType: String,
            returnBinding: (Any?) -> R
        ): R = error("Position requests must use metadata encoding")

        private fun position(netuid: Long) = io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct.Instance(
            mapOf(
                "netuid" to BigInteger.valueOf(netuid),
                "stake" to BigInteger.TEN,
                "hotkey" to ByteArray(32)
            )
        )
    }
}
