package io.novafoundation.nova.feature_swap_impl.data.assetExchange

import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicHash
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_swap_api.domain.model.MinimumBalanceBuyIn
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope

interface AssetExchange {

    interface Factory {

        suspend fun create(chainId: ChainId, coroutineScope: CoroutineScope): AssetExchange?
    }

    /**
     * Implementations should expect `asset` to be non-utility asset,
     * e.g. they don't need to additionally check whether asset is utility or not
     * They can also expect this method is called only when asset is present in [AssetExchange.availableSwapDirections]
     */
    suspend fun canPayFeeInNonUtilityToken(asset: Chain.Asset): Boolean

    suspend fun availableSwapDirections(): MultiMap<FullChainAssetId, FullChainAssetId>

    @Throws(SwapQuoteException::class)
    suspend fun quote(args: SwapQuoteArgs): AssetExchangeQuote

    suspend fun estimateFee(args: SwapExecuteArgs): AssetExchangeFee

    suspend fun swap(args: SwapExecuteArgs): Result<ExtrinsicHash>

    suspend fun slippageConfig(): SlippageConfig
}

class AssetExchangeQuote(
    val quote: Balance,
)

class AssetExchangeFee(
    val networkFee: Fee,
    val minimumBalanceBuyIn: MinimumBalanceBuyIn
)
