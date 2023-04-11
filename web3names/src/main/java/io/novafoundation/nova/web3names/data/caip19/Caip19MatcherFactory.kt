package io.novafoundation.nova.web3names.data.caip19

import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.Type.EvmErc20
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.Type.Unsupported
import io.novafoundation.nova.web3names.data.caip19.identifiers.Caip2Identifier
import io.novafoundation.nova.web3names.data.caip19.identifiers.Caip2Namespace
import io.novafoundation.nova.web3names.data.caip19.matchers.Caip19Matcher
import io.novafoundation.nova.web3names.data.caip19.matchers.asset.AssetMatcher
import io.novafoundation.nova.web3names.data.caip19.matchers.asset.Erc20AssetMatcher
import io.novafoundation.nova.web3names.data.caip19.matchers.asset.Slip44AssetMatcher
import io.novafoundation.nova.web3names.data.caip19.matchers.asset.UnsupportedAssetMatcher
import io.novafoundation.nova.web3names.data.caip19.matchers.caip2.Caip2Matcher
import io.novafoundation.nova.web3names.data.caip19.matchers.caip2.Caip2MatcherList
import io.novafoundation.nova.web3names.data.caip19.matchers.caip2.Eip155Matcher
import io.novafoundation.nova.web3names.data.caip19.matchers.caip2.SubstrateCaip2Matcher
import io.novafoundation.nova.web3names.data.caip19.repositories.Slip44CoinRepository

interface Caip19MatcherFactory {

    suspend fun getCaip19Matcher(chain: Chain, chainAsset: Chain.Asset): Caip19Matcher

    fun caip2Of(chain: Chain, preferredNamespace: Caip2Namespace): String?
}

class RealCaip19MatcherFactory(private val slip44CoinRepository: Slip44CoinRepository) :
    Caip19MatcherFactory {

    override suspend fun getCaip19Matcher(chain: Chain, chainAsset: Chain.Asset): Caip19Matcher {
        val caip2Matcher = getCaip2Matcher(chain)
        val assetNamespaceMatcher = getAssetNamespaceMatcher(chainAsset)

        return Caip19Matcher(caip2Matcher, assetNamespaceMatcher)
    }

    override fun caip2Of(chain: Chain, preferredNamespace: Caip2Namespace): String? {
       return when {
           chain.hasSubstrateRuntime && chain.isEthereumBased -> when(preferredNamespace) {
               Caip2Namespace.EIP155 -> eipChain(chain.addressPrefix)
               Caip2Namespace.POLKADOT -> polkadotChain(chain.genesisHash!!)
           }

           chain.hasSubstrateRuntime -> polkadotChain(chain.genesisHash!!)

           chain.isEthereumBased -> eipChain(chain.addressPrefix)

           else -> null
       }
    }

    private fun polkadotChain(genesisHash: String): String {
        return Caip2Identifier.Polkadot(genesisHash).namespaceWitId
    }

    private fun eipChain(eipChainId: Int): String {
        return Caip2Identifier.Eip155(eipChainId.toBigInteger()).namespaceWitId
    }

    private fun getCaip2Matcher(chain: Chain): Caip2Matcher {
        val matchers = buildList {
            add(SubstrateCaip2Matcher(chain))

            if (chain.isEthereumBased) {
                add(Eip155Matcher(chain))
            }
        }
        return Caip2MatcherList(matchers)
    }

    private suspend fun getAssetNamespaceMatcher(chainAsset: Chain.Asset): AssetMatcher {
        return when (val assetType = chainAsset.type) {
            is EvmErc20 -> Erc20AssetMatcher(assetType.contractAddress)

            Unsupported -> UnsupportedAssetMatcher()

            else -> slip44CoinRepository.getCoinCode(chainAsset)?.let(::Slip44AssetMatcher)
                ?: UnsupportedAssetMatcher()
        }
    }
}
