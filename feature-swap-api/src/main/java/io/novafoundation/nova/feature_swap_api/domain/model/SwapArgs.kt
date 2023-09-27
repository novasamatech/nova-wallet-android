package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SwapArgs(
    val assetIn: Chain.Asset,
    val assetOut: Chain.Asset,
    val amount: Balance,
    val swapDirection: SwapDirection,
    val slippage: Percent
)
