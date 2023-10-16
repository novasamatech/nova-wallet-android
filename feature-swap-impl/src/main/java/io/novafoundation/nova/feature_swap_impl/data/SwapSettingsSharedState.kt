package io.novafoundation.nova.feature_swap_impl.data

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_swap_api.domain.model.Slippage
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SelectedOptionSharedState
import kotlinx.coroutines.flow.MutableStateFlow

data class SwapSettings(
    val assetIn: Asset? = null,
    val assetOut: Asset? = null,
    val feeAsset: Chain.Asset? = null,
    val amount: Balance? = null,
    val swapDirection: SwapDirection? = null,
    val slippage: Percent = Slippage.DEFAULT
)

class SwapSettingsSharedState : SelectedOptionSharedState<SwapSettings> {

    override val selectedOption = MutableStateFlow(SwapSettings())

    fun setAssetIn(asset: Asset) {
        selectedOption.value = selectedOption.value.copy(assetIn = asset)
    }

    fun setAssetOut(asset: Asset) {
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

    fun setState(settings: SwapSettings) {
        selectedOption.value = settings
    }

    // TODO: computationState
    fun clear() {
        selectedOption.value = SwapSettings()
    }
}

fun SwapSettings.toQuoteArgs(): SwapQuoteArgs? {
    return try {
        SwapQuoteArgs(assetIn!!.token, assetOut!!.token, amount!!, swapDirection!!, slippage)
    } catch (e: Exception) {
        null
    }
}

fun SwapSettings.toExecuteArgs(): SwapExecuteArgs? {
    return try {
        SwapExecuteArgs(assetIn!!.token.configuration, assetOut!!.token.configuration, feeAsset, getSwapLimit()!!)
    } catch (e: Exception) {
        null
    }
}

fun SwapSettings.getSwapLimit(): SwapLimit? {
    return when (swapDirection) {
        SwapDirection.SPECIFIED_IN -> SwapLimit.SpecifiedIn(amount!!, amount!!) //TODO: calculate in and out amount
        SwapDirection.SPECIFIED_OUT -> SwapLimit.SpecifiedOut(amount!!, amount!!) //TODO: calculate in and out amount
        null -> null
    }
}
