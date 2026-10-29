package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRankedValidatorsRemote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorsRemote
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BittensorValidatorPreviewMappingTest {

    private val chain = mock(Chain::class.java)

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    init {
        `when`(chain.addressPrefix).thenReturn(42)
    }

    @Test
    fun `recommended preview preserves identity and trimmed name`() {
        for (recommendation in listOf("stable", "balanced", "higherUpside")) {
            val row = row(recommendation = recommendation)
            val result = mapBittensorValidatorPreview(chain, row)
            assertEquals(hotkey, result?.hotkey)
            assertEquals("Validator", result?.name)
        }
    }

    @Test
    fun `ineligible and unusual rows are not a suggested validator`() {
        assertNull(mapBittensorValidatorPreview(chain, row(eligible = false)))
        assertNull(mapBittensorValidatorPreview(chain, row(recommendation = "unusual")))
        assertNull(mapBittensorValidatorPreview(chain, row(recommendation = null)))
    }

    @Test
    fun `malformed address is not displayed as a real validator`() {
        assertNull(mapBittensorValidatorPreview(chain, row(address = "invalid")))
    }

    @Test
    fun `missing name keeps real address without inventing an identity`() {
        val address = chain.addressOf(hotkey)
        assertEquals(address, mapBittensorValidatorPreview(chain, row(name = " "))?.name)
    }

    @Test
    fun `recommended address decoding preserves cancellation and fatal errors`() {
        for (failure in listOf(CancellationException("Cancelled"), AssertionError("Decoder failure"))) {
            val remote = row(address = "hotkey")
            val decodingChain = mock(Chain::class.java)
            `when`(decodingChain.isEthereumBased).thenThrow(failure)

            assertSame(failure, runCatching { mapBittensorValidatorPreview(decodingChain, remote) }.exceptionOrNull())
        }
    }

    @Test
    fun `directory address decoding preserves cancellation and fatal errors`() {
        for (failure in listOf(CancellationException("Cancelled"), AssertionError("Decoder failure"))) {
            val remote = BittensorValidatorsRemote.Item(netuid = 64, "hotkey", "Validator", stakeSourceName = null)
            val decodingChain = mock(Chain::class.java)
            `when`(decodingChain.isEthereumBased).thenThrow(failure)

            assertSame(failure, runCatching { mapBittensorDirectoryPreview(decodingChain, remote) }.exceptionOrNull())
        }
    }

    private fun row(
        eligible: Boolean = true,
        recommendation: String? = "balanced",
        address: String = chain.addressOf(hotkey),
        name: String = " Validator "
    ) = BittensorRankedValidatorsRemote.Item(netuid = 64, address, name, eligible, recommendation, score = null)
}
