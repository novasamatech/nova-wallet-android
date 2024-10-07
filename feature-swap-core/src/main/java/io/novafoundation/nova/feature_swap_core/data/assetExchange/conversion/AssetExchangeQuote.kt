package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion

import io.novafoundation.nova.feature_swap_core.domain.model.QuotePath
import io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection
import java.math.BigInteger

class AssetExchangeQuote(
    val direction: SwapDirection,

    val quote: BigInteger,

    val path: QuotePath
) : Comparable<AssetExchangeQuote> {

    override fun compareTo(other: AssetExchangeQuote): Int {
        return when (direction) {
            // When we want to sell a token, the bigger the quote - the better
            SwapDirection.SPECIFIED_IN -> (quote - other.quote).signum()
            // When we want to buy a token, the smaller the quote - the better
            SwapDirection.SPECIFIED_OUT -> (other.quote - quote).signum()
        }
    }
}
