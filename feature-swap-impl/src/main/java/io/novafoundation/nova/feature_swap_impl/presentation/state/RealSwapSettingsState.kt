package io.novafoundation.nova.feature_swap_impl.presentation.state

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.flip
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SelectedOptionSharedState
import kotlinx.coroutines.flow.MutableStateFlow

class SwapSettingsState(
    initialValue: SwapSettings = SwapSettings()
) : SelectedOptionSharedState<SwapSettings> {

    override val selectedOption = MutableStateFlow(initialValue)

    fun setAssetInUpdatingFee(asset: Chain.Asset, chain: Chain) {
        val current = selectedOption.value

        val new = if (current.feeAsset == null || current.feeAsset.chainId != chain.id) {
            current.copy(assetIn = asset, feeAsset = chain.commissionAsset)
        } else {
            current.copy(assetIn = asset)
        }

        selectedOption.value = new
    }

    fun setAssetOut(asset: Chain.Asset) {
        selectedOption.value = selectedOption.value.copy(assetOut = asset)
    }

    fun setFeeAsset(asset: Chain.Asset) {
        selectedOption.value = selectedOption.value.copy(feeAsset = asset)
    }

    fun setAmount(amount: Balance?, swapDirection: SwapDirection) {
        selectedOption.value = selectedOption.value.copy(amount = amount, swapDirection = swapDirection)
    }

    fun setSlippage(slippage: Percent) {
        selectedOption.value = selectedOption.value.copy(slippage = slippage)
    }

    fun flipAssets(): SwapSettings {
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
