package io.novafoundation.nova.feature_swap_impl.presentation.state

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsState
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.flip
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.MutableStateFlow

class RealSwapSettingsState(
    initialValue: SwapSettings,
) : SwapSettingsState {

    override val selectedOption = MutableStateFlow(initialValue)

    override suspend fun setAssetInUpdatingFee(asset: Chain.Asset) {
        val current = selectedOption.value

        val newPlanks = current.convertedAmountForNewAssetIn(asset)
        val new = current.copy(assetIn = asset, amount = newPlanks)

        selectedOption.value = new
    }

    override fun setAssetOut(asset: Chain.Asset) {
        val current = selectedOption.value

        val newPlanks = current.convertedAmountForNewAssetOut(asset)

        selectedOption.value = selectedOption.value.copy(assetOut = asset, amount = newPlanks)
    }

    override fun setAmount(amount: Balance?, swapDirection: SwapDirection) {
        selectedOption.value = selectedOption.value.copy(amount = amount, swapDirection = swapDirection)
    }

    override fun setSlippage(slippage: Fraction) {
        selectedOption.value = selectedOption.value.copy(slippage = slippage)
    }

    override suspend fun flipAssets(): SwapSettings {
        val currentSettings = selectedOption.value

        val newSettings = currentSettings.copy(
            assetIn = currentSettings.assetOut,
            assetOut = currentSettings.assetIn,
            swapDirection = currentSettings.swapDirection?.flip()
        )
        selectedOption.value = newSettings

        return newSettings
    }

    override fun setSwapSettings(swapSettings: SwapSettings) {
        selectedOption.value = swapSettings
    }

    private fun SwapSettings.convertedAmountForNewAssetIn(newAssetIn: Chain.Asset): Balance? {
        val shouldConvertAsset = assetIn != null && amount != null && swapDirection == SwapDirection.SPECIFIED_IN

        return if (shouldConvertAsset) {
            val decimalAmount = assetIn!!.amountFromPlanks(amount!!)
            newAssetIn.planksFromAmount(decimalAmount)
        } else {
            amount
        }
    }

    private fun SwapSettings.convertedAmountForNewAssetOut(newAssetOut: Chain.Asset): Balance? {
        val shouldConvertAsset = assetOut != null && amount != null && swapDirection == SwapDirection.SPECIFIED_OUT

        return if (shouldConvertAsset) {
            val decimalAmount = assetOut!!.amountFromPlanks(amount!!)
            newAssetOut.planksFromAmount(decimalAmount)
        } else {
            amount
        }
    }
}
