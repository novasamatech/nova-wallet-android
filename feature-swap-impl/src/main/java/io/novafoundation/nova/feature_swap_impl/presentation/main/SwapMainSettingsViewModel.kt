package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.views.SwapAmountInputView
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class SwapButtonState(
    val state: ButtonState,
    val text: String
)

class SwapMainSettingsViewModel(
    private val swapRouter: SwapRouter
) : BaseViewModel() {

    // Placeholders
    val payInput: MutableSharedFlow<String> = MutableStateFlow("")
    val receiveInput: MutableSharedFlow<String> = MutableStateFlow("")

    val paymentAsset: Flow<SwapAmountInputView.Model> = flowOf { SwapAmountInputView.Model(null, "Pay", null, "Select a token") }
    val receivingAsset: Flow<SwapAmountInputView.Model> = flowOf { SwapAmountInputView.Model(null, "Receive", null, "Select a token") }
    val paymentTokenMaxAmount: Flow<String> = flowOf { "100 DOT" }
    val rateDetails: Flow<String> = flowOf { "1 USDT ≈ 0.21 DOT" }
    val networkFee: Flow<AmountModel> = flowOf { AmountModel("0.01524 DOT", "\$0.07") }
    val showDetails: Flow<Boolean> = flowOf { true }
    val buttonState: Flow<SwapButtonState> = flowOf { SwapButtonState(ButtonState.NORMAL, "Enter amount") }

    fun maxTokens() {
        TODO("Not yet implemented")
    }

    fun selectPayToken() {
        TODO("Not yet implemented")
    }

    fun selectReceiveToken() {
        TODO("Not yet implemented")
    }

    fun confirmButtonClicked() {
        swapRouter.openSwapConfirmation()
    }

    fun rateDetailsClicked() {
        TODO("Not yet implemented")
    }

    fun networkFeeClicked() {
        TODO("Not yet implemented")
    }

    fun flipAssets() {
        TODO("Not yet implemented")
    }
}
