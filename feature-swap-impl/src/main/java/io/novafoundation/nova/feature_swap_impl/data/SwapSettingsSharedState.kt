package io.novafoundation.nova.feature_swap_impl.data

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_swap_api.domain.model.Slippage
import io.novafoundation.nova.feature_swap_api.domain.model.SwapArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SelectedOptionSharedState
import kotlinx.coroutines.flow.MutableStateFlow

data class SwapSettings(
    val assetIn: Chain.Asset? = null,
    val assetOut: Chain.Asset? = null,
    val amount: Balance? = null,
    val swapDirection: SwapDirection? = null,
    val slippage: Percent = Slippage.DEFAULT
)

class SwapSettingsSharedState : SelectedOptionSharedState<SwapSettings> {

    override val selectedOption = MutableStateFlow(SwapSettings())

    fun setAssetIn(assetIn: Chain.Asset) {
        selectedOption.value = selectedOption.value.copy(assetIn = assetIn)
    }

    fun setAssetOut(assetOut: Chain.Asset) {
        selectedOption.value = selectedOption.value.copy(assetOut = assetOut)
    }

    fun setAmount(amount: Balance, swapDirection: SwapDirection) {
        selectedOption.value = selectedOption.value.copy(amount = amount, swapDirection = swapDirection)
    }

    fun setSlippage(slippage: Percent) {
        selectedOption.value = selectedOption.value.copy(slippage = slippage)
    }

    fun setState(settings: SwapSettings) {
        selectedOption.value = settings
    }

    // computationState
    fun clear() {
        selectedOption.value = SwapSettings()
    }
}

fun SwapSettings.toArgs(): SwapArgs? {
    return try {
        SwapArgs(assetIn!!, assetOut!!, amount!!, swapDirection!!, slippage)
    } catch (e: Exception) {
        null
    }
}
