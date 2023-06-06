package io.novafoundation.nova.caip.caip19

import io.novafoundation.nova.caip.caip19.matchers.Caip19Matcher
import io.novafoundation.nova.caip.caip19.matchers.asset.AssetMatcher
import io.novafoundation.nova.caip.caip19.matchers.asset.Erc20AssetMatcher
import io.novafoundation.nova.caip.caip19.matchers.asset.Slip44AssetMatcher
import io.novafoundation.nova.caip.caip19.matchers.asset.UnsupportedAssetMatcher
import io.novafoundation.nova.caip.caip2.Caip2MatcherFactory
import io.novafoundation.nova.caip.slip44.Slip44CoinRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.Type.EvmErc20
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.Type.Unsupported

interface Caip19MatcherFactory {

    suspend fun getCaip19Matcher(chain: Chain, chainAsset: Chain.Asset): Caip19Matcher
}

class RealCaip19MatcherFactory(
    private val slip44CoinRepository: Slip44CoinRepository,
    private val caip2MatcherFactory: Caip2MatcherFactory,
) : Caip19MatcherFactory {

    override suspend fun getCaip19Matcher(chain: Chain, chainAsset: Chain.Asset): Caip19Matcher {
        val caip2Matcher = caip2MatcherFactory.getCaip2Matcher(chain)
        val assetNamespaceMatcher = getAssetNamespaceMatcher(chainAsset)

        return Caip19Matcher(caip2Matcher, assetNamespaceMatcher)
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
