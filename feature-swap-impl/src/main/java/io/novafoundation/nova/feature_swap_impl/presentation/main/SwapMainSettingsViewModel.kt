package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.views.SwapAmountInputView
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.transferableFormat
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SwapButtonState(
    val state: ButtonState,
    val text: String
)

class SwapMainSettingsViewModel(
    private val swapRouter: SwapRouter,
    private val swapInteractor: SwapInteractor,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry
) : BaseViewModel() {

    private val swapSettings = swapInteractor.settings()

    private val swapQuote = swapInteractor.quotes()
        .map { it.getOrNull() }

    private val assetOutFlow = swapInteractor.assetOut()
    private val assetInFlow = swapInteractor.assetIn()

    val amountOutInput: MutableStateFlow<String> = MutableStateFlow("")
    val amountInInput: MutableStateFlow<String> = MutableStateFlow("")

    val amountOutFiat: Flow<String?> = assetOutFlow.combine(amountOutInput, ::mapInputToFiatAmount)
    val amountInFiat: Flow<String?> = assetInFlow.combine(amountInInput, ::mapInputToFiatAmount)

    val paymentAsset: Flow<SwapAmountInputView.Model> = swapSettings.map { formatInputAsset(it.assetOut, R.string.swap_field_asset_from_title) }
    val receivingAsset: Flow<SwapAmountInputView.Model> = swapSettings.map { formatInputAsset(it.assetIn, R.string.swap_field_asset_to_title) }

    val paymentTokenMaxAmount: Flow<String?> = assetOutFlow.map { it?.transferableFormat() }
    val rateDetails: Flow<String?> = flowOf { formatRate() }

    //TODO must depend on selected fee asset
    val networkFee: Flow<AmountModel?> = combine(assetOutFlow, swapQuote) { asset, quote ->
        asset ?: return@combine null
        quote ?: return@combine null
        mapAmountToAmountModel(quote.fee.onChainFee.amount, asset)
    }

    val showDetails: Flow<Boolean> = swapQuote.map { it != null }
    val buttonState: Flow<SwapButtonState> = flowOf { SwapButtonState(ButtonState.NORMAL, "Enter amount") }

    init {
        //TODO reuse it in mixin or component
        combine(assetOutFlow, amountOutInput) { assetOut, amount ->
            assetOut ?: return@combine null
            val planks = assetOut.token.planksFromAmount(amount.toBigDecimal())
            swapInteractor.setAmount(planks, SwapDirection.SPECIFIED_OUT)
        }.launchIn(this)

        combine(assetInFlow, amountInInput) { assetOut, amount ->
            assetOut ?: return@combine null
            val planks = assetOut.token.planksFromAmount(amount.toBigDecimal())
            swapInteractor.setAmount(planks, SwapDirection.SPECIFIED_IN)
        }.launchIn(this)

        swapQuote.onEach { quote ->
            quote ?: return@onEach
            setInputAmount(quote.assetIn, quote.amountIn, amountInInput)
            setInputAmount(quote.assetOut, quote.amountOut, amountOutInput)
        }.launchIn(this)
    }

    fun maxTokens() {
        TODO("Not yet implemented")
    }

    fun selectPayToken() {
        launch {
            val chain = chainRegistry.currentChains.first().first { it.assets.size > 1 }
            swapInteractor.setAssetOut(chain.assets[0])
        }
    }

    fun selectReceiveToken() {
        launch {
            val chain = chainRegistry.currentChains.first().first { it.assets.size > 1 }
            swapInteractor.setAssetIn(chain.assets[1])
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
        swapInteractor.flipAssets()
    }

    private suspend fun formatInputAsset(chainAsset: Chain.Asset?, titleRes: Int): SwapAmountInputView.Model {
        if (chainAsset == null) {
            return SwapAmountInputView.Model(
                null,
                resourceManager.getString(titleRes),
                null,
                resourceManager.getString(R.string.fragment_swap_main_settings_select_token),
                showInput = false
            )
        }

        val chain = chainRegistry.getChain(chainAsset.chainId)

        return SwapAmountInputView.Model(
            chainAsset.iconUrl,
            chainAsset.symbol,
            Icon.FromLink(chain.icon),
            chain.name,
            showInput = true
        )
    }

    private fun formatRate(): String {
        return ""
    }

    private fun String.toBigDecimal(): BigDecimal {
        return toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    private fun mapInputToFiatAmount(asset: Asset?, amount: String): String? {
        asset ?: return null
        val fiatAmount = asset.token.amountToFiat(amount.toBigDecimal())
        return fiatAmount.formatTokenAmount(asset.token.configuration)
    }

    private fun setInputAmount(chainAsset: Chain.Asset, planks: Balance, amountInInput: MutableStateFlow<String>) {
        val amount = chainAsset.amountFromPlanks(planks)
        amountInInput.value = amount.format()
    }
}
