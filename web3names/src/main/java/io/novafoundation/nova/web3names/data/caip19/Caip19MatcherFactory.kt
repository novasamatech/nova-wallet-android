package io.novafoundation.nova.web3names.data.caip19

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.Type.Evm
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.Type.Unsupported
import io.novafoundation.nova.web3names.data.caip19.repositories.Slip44CoinRepository
import io.novafoundation.nova.web3names.data.caip19.matchers.asset.AssetMatcher
import io.novafoundation.nova.web3names.data.caip19.matchers.Caip19Matcher
import io.novafoundation.nova.web3names.data.caip19.matchers.caip2.Caip2Matcher
import io.novafoundation.nova.web3names.data.caip19.matchers.asset.Erc20AssetMatcher
import io.novafoundation.nova.web3names.data.caip19.matchers.asset.Slip44AssetMatcher
import io.novafoundation.nova.web3names.data.caip19.matchers.asset.UnsupportedAssetMatcher
import io.novafoundation.nova.web3names.data.caip19.matchers.caip2.Caip2MatcherList
import io.novafoundation.nova.web3names.data.caip19.matchers.caip2.EthereumCaip2Matcher
import io.novafoundation.nova.web3names.data.caip19.matchers.caip2.SubstrateCaip2Matcher

interface Caip19MatcherFactory {

    fun getCaip19Matcher(chain: Chain, chainAsset: Chain.Asset): Caip19Matcher
}

class RealCaip19MatcherFactory(private val slip44CoinRepository: Slip44CoinRepository) :
    Caip19MatcherFactory {

    override fun getCaip19Matcher(chain: Chain, chainAsset: Chain.Asset): Caip19Matcher {
        val coip2Matcher = getCaip2Matcher(chain)
        val assetNamespaceMatcher = getAssetNamespaceMatcher(chainAsset)

        return Caip19Matcher(coip2Matcher, assetNamespaceMatcher)
    }

    private fun getCaip2Matcher(chain: Chain): Caip2Matcher {
        val matchers = buildList {
            add(SubstrateCaip2Matcher(chain))

            if (chain.isEthereumBased) {
                add(EthereumCaip2Matcher(chain))
            }
        }
        return Caip2MatcherList(matchers)
    }

    // TODO: support native evm
    private fun getAssetNamespaceMatcher(chainAsset: Chain.Asset): AssetMatcher {
        return when (chainAsset.type) {
            is Evm -> Erc20AssetMatcher(chainAsset)
            Unsupported -> UnsupportedAssetMatcher()
            else -> {
                val cointCode = slip44CoinRepository.getCoinCode(chainAsset)
                Slip44AssetMatcher(cointCode)
            }
        }
    }
}
