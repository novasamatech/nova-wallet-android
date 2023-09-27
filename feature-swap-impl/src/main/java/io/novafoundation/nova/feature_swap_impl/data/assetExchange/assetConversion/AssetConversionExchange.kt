package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicHash
import io.novafoundation.nova.feature_swap_api.domain.model.SwapArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class AssetConversionExchangeFactory() : AssetExchange.Factory {

    override fun create(chainId: ChainId): AssetExchange {
        return AssetConversionExchange(chainId)
    }
}

private class AssetConversionExchange(
    private val chainId: ChainId,
) : AssetExchange {

    override suspend fun availableSwapDirections(): Map<FullChainAssetId, Set<FullChainAssetId>> {
        TODO("Not yet implemented")
    }

    override suspend fun availableDirectionsForAsset(asset: Chain.Asset): Set<FullChainAssetId> {
        TODO("Not yet implemented")
    }

    override suspend fun quote(args: SwapArgs): Result<SwapQuote> {
        TODO("Not yet implemented")
    }

    override suspend fun swap(args: SwapArgs): Result<ExtrinsicHash> {
        TODO("Not yet implemented")
    }
}
