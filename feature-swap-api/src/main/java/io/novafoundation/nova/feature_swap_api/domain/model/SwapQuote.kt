package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SwapQuote(
    val amountIn: Balance,
    val assetIn: Chain.Asset,
    val amountOut: Balance,
    val assetOut: Chain.Asset,
    val priceImpact: Double,
    val fee: SwapFee
) {

    init {
        require(assetIn.chainId == assetOut.chainId) {
            "Cross-chain swaps are not yet implemented"
        }
    }
}

class SwapFee(
    val onChainFee: Fee
)
