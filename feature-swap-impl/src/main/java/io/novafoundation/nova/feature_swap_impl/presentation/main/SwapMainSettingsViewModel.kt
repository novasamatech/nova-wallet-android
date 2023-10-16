package io.novafoundation.nova.feature_swap_impl.presentation.main

import android.util.Log
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.UserEditableString
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.orFalse
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.data.SwapSettings
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.views.SwapAmountInputView
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.transferableFormat
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SwapButtonState(
    val state: ButtonState,
    val text: String
)

class SwapQuoteError(
    val buttonText: String,
    val highlightOutInput: Boolean,
    val highlightInInput: Boolean
)

class SwapMainSettingsViewModel(
    private val swapRouter: SwapRouter,
    private val swapInteractor: SwapInteractor,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry
) : BaseViewModel() {

    private val swapSettings = swapInteractor.settings()

    private val swapQuoteResult = swapInteractor.quotes()

    private val swapQuote = swapQuoteResult
        .map { it.getOrNull() }

    private val swapQuoteError = swapQuoteResult
        .map { formatQuoteError(it.exceptionOrNull()) }

    private val assetOutFlow = swapInteractor.assetOut()
    private val assetInFlow = swapInteractor.assetIn()
    private val feeAsset = swapInteractor.feeAsset()

    val amountOutInput: MutableStateFlow<UserEditableString> = MutableStateFlow(UserEditableString(""))
    val amountInInput: MutableStateFlow<UserEditableString> = MutableStateFlow(UserEditableString(""))

    val amountOutFiat: Flow<String?> = assetOutFlow.combine(amountOutInput, ::mapInputToFiatAmount)
        .shareInBackground()

    val amountInFiat: Flow<String?> = assetInFlow.combine(amountInInput, ::mapInputToFiatAmount)
        .shareInBackground()

    val paymentAsset: Flow<SwapAmountInputView.Model> = combine(swapQuoteError, swapSettings) { quoteError, settings ->
        formatInputAsset(settings.assetOut, quoteError?.highlightOutInput.orFalse(), R.string.swap_field_asset_from_title)
    }.shareInBackground()

    val receivingAsset: Flow<SwapAmountInputView.Model> = combine(swapQuoteError, swapSettings) { quoteError, settings ->
        formatInputAsset(settings.assetIn, quoteError?.highlightInInput.orFalse(), R.string.swap_field_asset_to_title)
    }.shareInBackground()

    val paymentTokenMaxAmount: Flow<String?> = assetOutFlow.map { it?.transferableFormat() }
        .shareInBackground()

    val rateDetails: Flow<String?> = flowOf { formatRate() }
        .shareInBackground()

    val networkFee: Flow<AmountModel?> = feeAsset.filterNotNull()
        .map { asset -> mapAmountToAmountModel(BigInteger.TEN, asset) }
        .shareInBackground()

    val showDetails: Flow<Boolean> = swapQuote.map { it != null }
        .shareInBackground()

    // TODO I guess we can move it to the validation
    val pairAvailabilityFlow: Flow<Boolean> = swapInteractor.observeAssetsPairAvailability(viewModelScope)

    val buttonState: Flow<SwapButtonState> = swapQuoteError.map {
        val buttonState = if (it == null) ButtonState.NORMAL else ButtonState.DISABLED
        SwapButtonState(buttonState, it?.buttonText ?: resourceManager.getString(R.string.common_continue))
    }.shareInBackground()

    init {
        handleInputChanges(assetOutFlow, amountOutInput, SwapDirection.SPECIFIED_OUT)
        handleInputChanges(assetInFlow, amountInInput, SwapDirection.SPECIFIED_IN)

        handleQuote()
    }

    //TODO sync with iOS team
    fun maxTokens() {

    }

    fun selectPayToken() {
        launch {
            val assets = swapInteractor.availableAssets(viewModelScope)
            swapInteractor.setAssetOut(assets[1])
        }
    }

    fun selectReceiveToken() {
        launch {
            val assets = swapInteractor.availableAssets(viewModelScope)
            swapInteractor.setAssetIn(assets[0])
        }
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
        val newSettings = swapInteractor.flipAssets()

        updateSwapSettingsAmountSilently(newSettings)
    }

    private fun updateSwapSettingsAmountSilently(swapSettings: SwapSettings) {
        if (swapSettings.amount == null) return

        when (swapSettings.swapDirection) {
            SwapDirection.SPECIFIED_IN -> {
                swapSettings.assetIn ?: return
                setInputAmountSilently(swapSettings.assetIn.token.configuration, swapSettings.amount, amountInInput)
            }

            SwapDirection.SPECIFIED_OUT -> {
                swapSettings.assetOut ?: return
                setInputAmountSilently(swapSettings.assetOut.token.configuration, swapSettings.amount, amountOutInput)
            }

            else -> {}
        }
    }

    private suspend fun formatInputAsset(asset: Asset?, isError: Boolean, titleRes: Int): SwapAmountInputView.Model {
        val chainAsset = asset?.token?.configuration
        if (chainAsset == null) {
            return defaultInputModel(titleRes)
        }

        val chain = chainRegistry.getChain(chainAsset.chainId)

        return SwapAmountInputView.Model(
            chainAsset.iconUrl,
            chainAsset.symbol,
            Icon.FromLink(chain.icon),
            chain.name,
            showInput = true,
            isError = isError
        )
    }

    private fun defaultInputModel(titleRes: Int): SwapAmountInputView.Model {
        return SwapAmountInputView.Model(
            null,
            resourceManager.getString(titleRes),
            null,
            resourceManager.getString(R.string.fragment_swap_main_settings_select_token),
            showInput = false,
            isError = false
        )
    }

    private fun formatRate(): String {
        return ""
    }

    private fun String.toBigDecimal(): BigDecimal {
        return toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    private fun mapInputToFiatAmount(asset: Asset?, amount: UserEditableString): String? {
        asset ?: return null
        val fiatAmount = asset.token.amountToFiat(amount.value.toBigDecimal())
        return fiatAmount.formatAsCurrency(asset.token.currency)
    }

    private fun setInputAmountSilently(chainAsset: Chain.Asset, planks: Balance, amountInput: MutableStateFlow<UserEditableString>) {
        val amount = chainAsset.amountFromPlanks(planks)
        amountInput.value = UserEditableString(amount.format(), editedByUser = false)
    }

    private fun handleQuote() {
        swapQuoteResult.onEach { result ->
            if (result.isSuccess) {
                val quote = result.getOrThrow()
                when (quote?.direction) {
                    null -> clearFieldsSilently()
                    SwapDirection.SPECIFIED_OUT -> setInputAmountSilently(quote.assetIn, quote.planksIn, amountInInput)
                    SwapDirection.SPECIFIED_IN -> setInputAmountSilently(quote.assetOut, quote.planksOut, amountOutInput)
                }
            } else {
                val settings = swapSettings.first()
                when (settings.swapDirection) {
                    SwapDirection.SPECIFIED_OUT -> clearFieldSilently(amountInInput)
                    SwapDirection.SPECIFIED_IN -> clearFieldSilently(amountOutInput)
                    else -> {}
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun handleInputChanges(assetFlow: Flow<Asset?>, amountInput: MutableStateFlow<UserEditableString>, swapDirection: SwapDirection) {
        amountInput.onEach {
            val asset = assetFlow.first()
            if (asset == null) return@onEach
            if (!it.editedByUser) return@onEach
            val amount = it.value.toBigDecimalOrNull()
            val planks = amount?.let { asset.token.planksFromAmount(amount) }
            swapInteractor.setAmount(planks, swapDirection)
        }.launchIn(viewModelScope)
    }

    private fun clearFieldsSilently() {
        clearFieldSilently(amountInInput)
        clearFieldSilently(amountOutInput)
    }

    private fun clearFieldSilently(amountInput: MutableStateFlow<UserEditableString>) {
        amountInput.value = UserEditableString("", editedByUser = false)
    }

    private fun formatQuoteError(exception: Throwable?): SwapQuoteError? {
        if (exception == null) return null

        return SwapQuoteError(
            "", false, false
        )
    }
}
