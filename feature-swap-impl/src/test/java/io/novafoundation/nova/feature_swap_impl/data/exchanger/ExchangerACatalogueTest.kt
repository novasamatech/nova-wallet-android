package io.novafoundation.nova.feature_swap_impl.data.exchanger

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.catalogue.RealExchangerATaoCatalogue
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ExchangerACatalogueTest {

    private val gson = Gson()

    private val source = mock(ExchangerAQuoteSource::class.java)

    private val registry = mock(ChainRegistry::class.java)

    private val repository = ExchangerAAssetsRepository(source, registry)

    private val catalogue = RealExchangerATaoCatalogue(repository)

    private val dot = asset(Chain.Geneses.POLKADOT_ASSET_HUB, 0)

    private val usdt = asset(Chain.Geneses.POLKADOT_ASSET_HUB, 1)

    private val usdc = asset(Chain.Geneses.POLKADOT_ASSET_HUB, 2)

    private val tao = asset(Chain.Geneses.BITTENSOR, 0)

    private val eth = asset("eip155:1", 0)

    private val assetHub = chain(dot.chainId, listOf(dot, usdt, usdc))

    private val bittensor = chain(tao.chainId, listOf(tao))

    private val ethereum = chain(eth.chainId, listOf(eth))

    private val chains = MutableStateFlow(listOf(assetHub, bittensor, ethereum))

    @Before
    fun setup() {
        `when`(registry.currentChains).thenReturn(chains)
    }

    @Test
    fun `catalogue resolves wallet assets and preserves separate direction flags`() = runBlocking {
        respond(remote(usdt, send = false), remote(tao, receive = false), remote(eth))

        val result = repository.assets()

        assertEquals(listOf(usdt, tao, eth), result.map { it.chainAsset })
        assertSame(usdt, result.first().chainAsset)
        assertFalse(result[0].canSend)
        assertTrue(result[0].canReceive)
        assertTrue(result[1].canSend)
        assertFalse(result[1].canReceive)
    }

    @Test
    fun `all send-enabled Asset Hub assets can be inputs into TAO without an asset allowlist`() = runBlocking {
        val newAsset = asset(dot.chainId, 42)
        val receiveOnlyAsset = asset(dot.chainId, 43)
        chains.value = listOf(chain(dot.chainId, listOf(dot, usdt, usdc, newAsset, receiveOnlyAsset)), bittensor, ethereum)
        respond(remote(eth), remote(tao), remote(usdc), remote(dot), remote(usdt), remote(newAsset), remote(receiveOnlyAsset, send = false))

        val pairs = catalogue.availablePairs()

        assertEquals(listOf(usdc, dot, usdt, newAsset), pairs.map { it.assetIn })
        assertTrue(pairs.all { it.assetOut === tao })
    }

    @Test
    fun `pair check uses current send and receive flags and rejects the reverse direction`() = runBlocking {
        respond(remote(usdt), remote(tao))
        assertTrue(catalogue.supportsPair(usdt.fullId, tao.fullId))
        assertFalse(catalogue.supportsPair(tao.fullId, usdt.fullId))
        respond(remote(usdt, send = false), remote(tao))
        assertFalse(catalogue.supportsPair(usdt.fullId, tao.fullId))
    }

    @Test
    fun `receive-only input is excluded and send-only input remains usable`() = runBlocking {
        respond(remote(dot, send = false), remote(usdt, receive = false), remote(tao, send = false))

        val pairs = catalogue.availablePairs()

        assertEquals(listOf(usdt), pairs.map { it.assetIn })
    }

    @Test
    fun `absent or receive-disabled TAO leaves no candidate pairs`() = runBlocking {
        respond(remote(dot))
        assertTrue(catalogue.availablePairs().isEmpty())

        respond(remote(dot), remote(tao, receive = false))
        assertTrue(catalogue.availablePairs().isEmpty())
    }

    @Test
    fun `empty backend catalogue stays empty`() = runBlocking {
        respond()

        assertTrue(repository.assets().isEmpty())
        assertTrue(catalogue.availablePairs().isEmpty())
    }

    @Test
    fun `unknown chain and unknown asset are skipped without losing known assets`() = runBlocking {
        respond(
            ExchangerAAssetRemote("unknown", 0, true, true),
            ExchangerAAssetRemote(dot.chainId, 999, true, true),
            remote(dot),
            remote(tao)
        )

        assertEquals(listOf(dot, tao), repository.assets().map { it.chainAsset })
        assertEquals(listOf(dot), catalogue.availablePairs().map { it.assetIn })
    }

    @Test
    fun `disabled paying or receiving network cannot produce a pair`() = runBlocking {
        respond(remote(dot), remote(tao))
        chains.value = listOf(chain(dot.chainId, listOf(dot), enabled = false), bittensor)
        assertTrue(catalogue.availablePairs().isEmpty())

        chains.value = listOf(assetHub, chain(tao.chainId, listOf(tao), enabled = false))
        assertTrue(catalogue.availablePairs().isEmpty())
    }

    @Test
    fun `asset identity determines release scope even with the same symbol`() = runBlocking {
        val anotherDot = asset("another-chain", 0)
        `when`(anotherDot.symbol).thenReturn(TokenSymbol("DOT"))
        `when`(dot.symbol).thenReturn(TokenSymbol("DOT"))
        chains.value = chains.value + chain(anotherDot.chainId, listOf(anotherDot))
        respond(remote(anotherDot), remote(dot), remote(tao))

        assertEquals(listOf(dot), catalogue.availablePairs().map { it.assetIn })
    }

    @Test
    fun `subsequent loads use current backend availability and chain configuration`() = runBlocking {
        respond(remote(dot), remote(tao))
        assertEquals(1, catalogue.availablePairs().size)

        respond(remote(dot, send = false), remote(tao))
        assertTrue(catalogue.availablePairs().isEmpty())

        respond(remote(dot), remote(tao))
        chains.value = listOf(bittensor)
        assertTrue(catalogue.availablePairs().isEmpty())
    }

    @Test
    fun `missing catalogue fields fail instead of silently enabling native assets`() = runBlocking {
        val valid = """{"chainId":"${dot.chainId}","assetId":0,"send":true,"receive":true}"""
        val invalidResponses = listOf(
            valid.replace("\"assetId\":0,", ""),
            valid.replace("\"send\":true,", ""),
            valid.replace(",\"receive\":true", ""),
            valid.replace("\"chainId\":\"${dot.chainId}\",", "")
        )
        for (json in invalidResponses) {
            val asset = gson.fromJson(json, ExchangerAAssetRemote::class.java)
            `when`(source.assets()).thenReturn(listOf(asset))

            assertTrue(runCatching { repository.assets() }.exceptionOrNull() is IOException)
        }
    }

    @Test
    fun `transport failures and cancellation remain failures rather than empty catalogues`() = runBlocking {
        for (expected in listOf(IOException("Unavailable"), CancellationException("Cancelled"))) {
            doAnswer { throw expected }.`when`(source).assets()

            assertSame(expected, runCatching { catalogue.availablePairs() }.exceptionOrNull())
        }
    }

    private suspend fun respond(vararg assets: ExchangerAAssetRemote) {
        `when`(source.assets()).thenReturn(assets.toList())
    }

    private fun remote(asset: Chain.Asset, send: Boolean = true, receive: Boolean = true) =
        ExchangerAAssetRemote(asset.chainId, asset.id, send, receive)

    private fun asset(chainId: String, id: Int): Chain.Asset {
        val asset = mock(Chain.Asset::class.java)
        `when`(asset.chainId).thenReturn(chainId)
        `when`(asset.id).thenReturn(id)
        return asset
    }

    private fun chain(id: String, assets: List<Chain.Asset>, enabled: Boolean = true): Chain {
        val chain = mock(Chain::class.java)
        val assetsById = assets.associateBy { it.id }
        `when`(chain.id).thenReturn(id)
        `when`(chain.assetsById).thenReturn(assetsById)
        `when`(chain.connectionState).thenReturn(if (enabled) Chain.ConnectionState.FULL_SYNC else Chain.ConnectionState.DISABLED)
        return chain
    }
}
