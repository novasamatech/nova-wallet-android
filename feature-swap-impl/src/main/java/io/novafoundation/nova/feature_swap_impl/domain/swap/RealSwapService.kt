package io.novafoundation.nova.feature_swap_impl.domain.swap

import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicHash
import io.novafoundation.nova.feature_swap_api.domain.model.SwapArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetConversionExchangeFactory
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class RealSwapService(
    assetConversionFactory: AssetConversionExchangeFactory,
) : SwapService {

    private val exchanges = mapOf(
        assetConversionFactory inChain Chain.Geneses.STATEMINE,
        assetConversionFactory inChain Chain.Geneses.STATEMINT
    )

    override suspend fun assetsAvailableForSwap(): Set<FullChainAssetId> = withContext(Dispatchers.Default) {
        exchanges.flatMapTo(mutableSetOf()) { (_, exchange) ->
            exchange.availableSwapDirections().keys
        }
    }

    override suspend fun availableSwapDirectionsFor(asset: Chain.Asset): Set<FullChainAssetId> = withContext(Dispatchers.Default) {
        exchanges[asset.chainId]?.availableDirectionsForAsset(asset).orEmpty()
    }

    override suspend fun quote(args: SwapArgs): Result<SwapQuote> {
        return runCatching { exchanges.getValue(args.assetIn.chainId) }
            .flatMap { exchange -> exchange.quote(args) }
    }

    override suspend fun swap(args: SwapArgs): Result<ExtrinsicHash> {
        return runCatching { exchanges.getValue(args.assetIn.chainId) }
            .flatMap { exchange -> exchange.swap(args) }
    }

    private infix fun AssetExchange.Factory.inChain(chainId: ChainId): Pair<ChainId, AssetExchange> {
        return chainId to create(chainId)
    }
}
