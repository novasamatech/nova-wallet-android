package io.novafoundation.nova.feature_swap_impl.domain.validation.utils

import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.quotedAmount
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.coroutineContext

class SharedQuoteValidationRetriever(
    private val swapService: SwapService,
    private val assetsValidationContext: AssetsValidationContext,
) {

    private var result: Result<SwapQuote>? = null

    suspend fun retrieveQuote(value: SwapValidationPayload): Result<SwapQuote> {
        if (result == null) {
            val assetIn = assetsValidationContext.getAsset(value.amountIn.chainAsset)
            val assetOut = assetsValidationContext.getAsset(value.amountOut.chainAsset)

            val amount = value.swapQuote.quotedPath.quotedAmount
            val direction = value.swapQuote.direction

            val quoteArgs = SwapQuoteArgs(assetIn.token, assetOut.token, amount, direction)

            result = swapService.quote(quoteArgs, CoroutineScope(coroutineContext))
        }

        return result!!
    }
}
