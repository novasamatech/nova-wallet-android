package io.novafoundation.nova.feature_swap_impl.presentation.state

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsState
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.flip
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.MutableStateFlow

class RealSwapSettingsState(
    initialValue: SwapSettings = SwapSettings()
) : SwapSettingsState {

    override val selectedOption = MutableStateFlow(initialValue)

    override fun setAssetInUpdatingFee(asset: Chain.Asset, chain: Chain) {
        val current = selectedOption.value

        val new = if (current.feeAsset == null || current.feeAsset!!.chainId != chain.id) {
            current.copy(assetIn = asset, feeAsset = chain.commissionAsset)
        } else {
            current.copy(assetIn = asset)
        }

        selectedOption.value = new
    }

    override fun setAssetOut(asset: Chain.Asset) {
        selectedOption.value = selectedOption.value.copy(assetOut = asset)
    }

    override fun setFeeAsset(asset: Chain.Asset) {
        selectedOption.value = selectedOption.value.copy(feeAsset = asset)
    }

    override fun setAmount(amount: Balance?, swapDirection: SwapDirection) {
        selectedOption.value = selectedOption.value.copy(amount = amount, swapDirection = swapDirection)
    }

    override fun setSlippage(slippage: Percent) {
        selectedOption.value = selectedOption.value.copy(slippage = slippage)
    }

    override fun flipAssets(): SwapSettings {
        val currentSettings = selectedOption.value
        val newSettings = currentSettings.copy(
            assetIn = currentSettings.assetOut,
            assetOut = currentSettings.assetIn,
            swapDirection = currentSettings.swapDirection?.flip()
        )
        selectedOption.value = newSettings

        return newSettings
    }
}
