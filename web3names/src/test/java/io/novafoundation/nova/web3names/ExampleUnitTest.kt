package io.novafoundation.nova.web3names

import io.novafoundation.nova.common.utils.requireValue
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.web3names.domain.caip19.Caip19Parser
import io.novafoundation.nova.web3names.domain.caip19.RealCaip19MatcherFactory
import io.novafoundation.nova.web3names.domain.caip19.identifiers.Caip19Identifier
import io.novafoundation.nova.web3names.domain.caip19.repositories.Slip44CoinRepository
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

class ExampleUnitTest {

    private val coipCaip19MatcherFactory = RealCaip19MatcherFactory(getSlip44CoinRepository())
    private val parser = Caip19Parser()

    private val cointType = 1

    private val chainAddress = "0x0"

    private val chain = Chain(
        id = chainAddress,
        name = "name",
        assets = emptyList(),
        nodes = emptyList(),
        explorers = emptyList(),
        externalApis = emptyList(),
        icon = "icon",
        addressPrefix = 0,
        types = null,
        isEthereumBased = false,
        isTestNet = false,
        hasCrowdloans = false,
        governance = emptyList(),
        parentId = "parentId",
        additional = null,
    )

    private val chainAsset = Chain.Asset(
        id = 0,
        iconUrl = "iconUrl",
        priceId = "priceId",
        chainId = "chainId",
        symbol = "symbol",
        precision = 8,
        buyProviders = emptyMap(),
        staking = Chain.Asset.StakingType.UNSUPPORTED,
        type = Chain.Asset.Type.Native,
        source = Chain.Asset.Source.DEFAULT,
        name = "name",
        enabled = true,
    )

    @Test
    fun polkadot_slip44_should_match() {
        val identifier = getIdentifier("polkadot:$chainAddress/slip44:$cointType")
        val matcher = coipCaip19MatcherFactory.getCaip19Matcher(chain, chainAsset)
        assertTrue(matcher.match(identifier))
    }

    @Test
    fun polkadot_slip44_should_not_match() {
        val chainId = "eip155:1"
        val evmChain = chain.copy(isEthereumBased = true, id = chainId)
        val erc20Asset = chainAsset.copy(type = Chain.Asset.Type.Evm("0x0"))
        val identifier = getIdentifier("polkadot:$chainAddress/slip44:$cointType")
        val matcher = coipCaip19MatcherFactory.getCaip19Matcher(evmChain, erc20Asset)
        assertFalse(matcher.match(identifier))
    }

    @Test
    fun eip155_erc20_should_match() {
        val chainId = "eip155:$cointType"
        val evmChain = chain.copy(isEthereumBased = true, id = chainId, addressPrefix = cointType)
        val erc20Asset = chainAsset.copy(type = Chain.Asset.Type.Evm("0x0"))
        val identifier = getIdentifier("$chainId/erc20:0x0")
        val matcher = coipCaip19MatcherFactory.getCaip19Matcher(evmChain, erc20Asset)
        assertTrue(matcher.match(identifier))
    }

    @Test
    fun eip155_erc20_wrong_coinType() {
        val chainId = "eip155:$cointType"
        val evmChain = chain.copy(isEthereumBased = true, id = chainId, addressPrefix = Int.MAX_VALUE)
        val erc20Asset = chainAsset.copy(type = Chain.Asset.Type.Evm("0x0"))
        val identifier = getIdentifier("$chainId/erc20:0x0")
        val matcher = coipCaip19MatcherFactory.getCaip19Matcher(evmChain, erc20Asset)
        assertFalse(matcher.match(identifier))
    }

    @Test
    fun eip155_erc20_should_not_match() {
        val identifier = getIdentifier("eip155:$cointType/erc20:0x0")
        val matcher = coipCaip19MatcherFactory.getCaip19Matcher(chain, chainAsset)
        assertFalse(matcher.match(identifier))
    }

    private fun getSlip44CoinRepository(): Slip44CoinRepository {
        return object : Slip44CoinRepository {
            override fun getCoinCode(chainAsset: Chain.Asset): Int {
                return cointType
            }
        }
    }

    private fun getIdentifier(raw: String): Caip19Identifier {
        return parser.parseCaip19(raw).requireValue()
    }
}
