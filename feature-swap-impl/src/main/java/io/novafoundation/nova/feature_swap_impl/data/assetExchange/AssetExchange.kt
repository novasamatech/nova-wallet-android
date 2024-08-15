package io.novafoundation.nova.feature_swap_impl.data.assetExchange

import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_api.domain.model.MinimumBalanceBuyIn
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_core.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetExchangeQuote
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetExchangeQuoteArgs
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface AssetExchange {

    interface Factory {

        suspend fun create(chain: Chain, coroutineScope: CoroutineScope): AssetExchange?
    }

    suspend fun sync()

    /**
     * Implementations should expect `asset` to be non-utility asset,
     * e.g. they don't need to additionally check whether asset is utility or not
     * They can also expect this method is called only when asset is present in [AssetExchange.availableSwapDirections]
     */
    suspend fun canPayFeeInNonUtilityToken(asset: Chain.Asset): Boolean

    suspend fun availableSwapDirections(): MultiMap<FullChainAssetId, FullChainAssetId>

    @Throws(SwapQuoteException::class)
    suspend fun quote(args: AssetExchangeQuoteArgs): AssetExchangeQuote

    suspend fun estimateFee(args: SwapExecuteArgs): AssetExchangeFee

    suspend fun swap(args: SwapExecuteArgs): Result<ExtrinsicSubmission>

    suspend fun slippageConfig(): SlippageConfig

    fun runSubscriptions(chain: Chain, metaAccount: MetaAccount): Flow<ReQuoteTrigger>
}

class AssetExchangeFee(
    val networkFee: Fee,
    val minimumBalanceBuyIn: MinimumBalanceBuyIn
)
