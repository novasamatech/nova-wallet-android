package io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev

import io.novafoundation.nova.common.data.network.runtime.model.SignedBlock
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mev.RemoteBittensorMevSource
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BittensorMevTimingTest {

    private val storage = mock(StorageDataSource::class.java)

    private val rpc = mock(RpcCalls::class.java)

    private val chain = mock(Chain::class.java)

    private val source = RemoteBittensorMevSource(storage, rpc)

    @Test
    fun `era is exactly eight blocks and anchored to finalized head with five blocks of lag allowed`() = runTest {
        configure(105)
        val timing = source.timing(chain, "address")
        assertEquals(100, timing.birth)
        assertEquals(8, timing.mortality.era.period)
        assertEquals(4, timing.mortality.era.phase)
        assertEquals("0xfinal", timing.mortality.blockHash)
        assertEquals(31.toBigInteger(), timing.nonce)
    }

    @Test
    fun `six blocks of finality lag refuses an order that cannot land in its era`() = runTest {
        configure(106)
        assertTrue(runCatching { source.timing(chain, "address") }.exceptionOrNull() is IllegalStateException)
    }

    private suspend fun configure(best: Int) {
        `when`(chain.id).thenReturn("chain")
        `when`(rpc.getNonce("chain", "address")).thenReturn(31.toBigInteger())
        `when`(rpc.getFinalizedHead("chain")).thenReturn("0xfinal")
        `when`(rpc.getBlockHeader("chain", "0xfinal")).thenReturn(header(100))
        `when`(rpc.getBlockHeader("chain")).thenReturn(header(best))
    }

    private fun header(height: Int) = SignedBlock.Block.Header("0x" + height.toString(16), null)
}
