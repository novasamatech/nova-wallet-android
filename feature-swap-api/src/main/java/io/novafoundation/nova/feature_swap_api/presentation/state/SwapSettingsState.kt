package io.novafoundation.nova.feature_swap_api.presentation.state

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SelectedOptionSharedState

interface SwapSettingsState : SelectedOptionSharedState<SwapSettings> {

    suspend fun setAssetIn(asset: Chain.Asset)

    fun setAssetOut(asset: Chain.Asset)

    fun setAmount(amount: Balance?, swapDirection: SwapDirection)

    fun setSlippage(slippage: Fraction)

    suspend fun flipAssets(): SwapSettings

    fun setSwapSettings(swapSettings: SwapSettings)
}
