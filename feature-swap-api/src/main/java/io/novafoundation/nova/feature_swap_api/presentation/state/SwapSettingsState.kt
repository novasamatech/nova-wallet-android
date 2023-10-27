package io.novafoundation.nova.feature_swap_api.presentation.state

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SelectedOptionSharedState

interface SwapSettingsState : SelectedOptionSharedState<SwapSettings> {

    fun setAssetInUpdatingFee(asset: Chain.Asset, chain: Chain)

    fun setAssetOut(asset: Chain.Asset)

    fun setFeeAsset(asset: Chain.Asset)

    fun setAmount(amount: Balance?, swapDirection: SwapDirection)

    fun setSlippage(slippage: Percent)

    fun flipAssets(): SwapSettings
}
