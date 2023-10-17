package io.novafoundation.nova.feature_swap_api.data

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_swap_api.domain.model.Slippage
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

data class SwapSettings(
    val assetIn: Asset? = null,
    val assetOut: Asset? = null,
    val feeAsset: Chain.Asset? = null,
    val amount: Balance? = null,
    val swapDirection: SwapDirection? = null,
    val slippage: Percent = Slippage.DEFAULT
)

interface SwapSettingsState {

    fun setAssetIn(asset: Asset)
    fun setAssetOut(asset: Asset)

    fun setFeeAsset(asset: Chain.Asset)

    fun setAmount(amount: Balance?, swapDirection: SwapDirection)

    fun setSlippage(slippage: Percent)

    fun setState(settings: SwapSettings)
}
