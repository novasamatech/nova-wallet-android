package io.novafoundation.nova.caip

import io.novafoundation.nova.caip.caip19.RealCaip19MatcherFactory
import io.novafoundation.nova.caip.caip19.RealCaip19Parser
import io.novafoundation.nova.caip.caip19.identifiers.Caip19Identifier
import io.novafoundation.nova.caip.caip2.RealCaip2MatcherFactory
import io.novafoundation.nova.caip.caip2.RealCaip2Parser
import io.novafoundation.nova.caip.slip44.Slip44CoinRepository
import io.novafoundation.nova.common.utils.requireValue
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class Caip19MatcherTest {

    private val caip19MatcherFactory = RealCaip19MatcherFactory(getSlip44CoinRepository(), RealCaip2MatcherFactory())
    private val parser = RealCaip19Parser(RealCaip2Parser())

    private val cointType = 1

    private val substrateChainId = "0x0"
    private val ethereumChainId = "eip155:1"

    @Mock
    private lateinit var chain: Chain

    @Mock
    private lateinit var chainAsset: Chain.Asset

    @Test
    fun `polkadot slip44 should match`() = runBlocking {
        mockChain(chainId = substrateChainId, isEthereumBased = false)
        mockAsset(type = Chain.Asset.Type.Native)
        val identifier = getIdentifier("polkadot:$substrateChainId/slip44:$cointType")
        val matcher = caip19MatcherFactory.getCaip19Matcher(chain, chainAsset)
        assertTrue(matcher.match(identifier))
    }

    @Test
    fun `polkadot slip44 should not match`() = runBlocking {
        mockChain(chainId = "eip155:1", isEthereumBased = true)
        mockAsset(type = Chain.Asset.Type.EvmErc20("0x0"))
        val identifier = getIdentifier("polkadot:${substrateChainId}/slip44:$cointType")
        val matcher = caip19MatcherFactory.getCaip19Matcher(chain, chainAsset)
        assertFalse(matcher.match(identifier))
    }

    @Test
    fun `eip155 erc20 should match`() = runBlocking {
        mockChain(chainId = ethereumChainId, isEthereumBased = true)
        mockAsset(type = Chain.Asset.Type.EvmErc20("0x0"))
        val identifier = getIdentifier("$ethereumChainId/erc20:0x0")
        val matcher = caip19MatcherFactory.getCaip19Matcher(chain, chainAsset)
        assertTrue(matcher.match(identifier))
    }

    @Test
    fun `eip155 erc20 wrong coinType`() = runBlocking {
        mockChain(chainId = ethereumChainId, isEthereumBased = true)
        mockAsset(type = Chain.Asset.Type.EvmErc20("0x0"))
        val identifier = getIdentifier("eip155:3/erc20:0x0")
        val matcher = caip19MatcherFactory.getCaip19Matcher(chain, chainAsset)
        assertFalse(matcher.match(identifier))
    }

    @Test
    fun `eip155 erc20 should not match`() = runBlocking {
        mockChain(chainId = substrateChainId, isEthereumBased = false)
        mockAsset(type = Chain.Asset.Type.Native)
        val identifier = getIdentifier("eip155:$cointType/erc20:0x0")
        val matcher = caip19MatcherFactory.getCaip19Matcher(chain, chainAsset)
        assertFalse(matcher.match(identifier))
    }

    private fun getSlip44CoinRepository(): Slip44CoinRepository {
        return object : Slip44CoinRepository {
            override suspend fun getCoinCode(chainAsset: Chain.Asset): Int {
                return cointType
            }
        }
    }

    private fun getIdentifier(raw: String): Caip19Identifier {
        return parser.parseCaip19(raw).requireValue()
    }

    private fun mockChain(chainId: String, isEthereumBased: Boolean) {
        `when`(chain.id).thenReturn(chainId)
        `when`(chain.isEthereumBased).thenReturn(isEthereumBased)
    }

    private fun mockAsset(type: Chain.Asset.Type) {
        `when`(chainAsset.type).thenReturn(type)
    }
}
