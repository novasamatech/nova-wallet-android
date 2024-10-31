package io.novafoundation.nova.feature_swap_api.presentation.state

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.percents
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

val DEFAULT_SLIPPAGE = 0.5.percents

data class SwapSettings(
    val assetIn: Chain.Asset? = null,
    val assetOut: Chain.Asset? = null,
    val amount: Balance? = null,
    val swapDirection: SwapDirection? = null,
    val slippage: Fraction
)
