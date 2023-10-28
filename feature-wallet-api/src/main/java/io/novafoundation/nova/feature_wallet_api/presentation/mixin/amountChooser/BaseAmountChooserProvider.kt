package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.firstNotNull
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState.InputKind
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider.MaxAvailableForAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.time.Duration.Companion.milliseconds

private const val DEBOUNCE_DURATION_MILLIS = 500

@OptIn(FlowPreview::class)
@Suppress("LeakingThis")
open class BaseAmountChooserProvider(
    coroutineScope: CoroutineScope,
    tokenFlow: Flow<Token?>,
    private val maxActionProvider: MaxActionProvider?
) : AmountChooserMixinBase.Presentation,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    private val chainAssetFlow = tokenFlow.map { it?.configuration }
        .distinctUntilChanged()

    final override val inputState = MutableStateFlow(defaultState())

    @Deprecated(
        message = "Use `inputState` instead",
        replaceWith = ReplaceWith(
            expression = "inputState.map { it.value }",
            imports = ["kotlinx.coroutines.flow.map"]
        )
    )
    final override val amountInput = inputState.map { it.value }
        .stateIn(this, SharingStarted.Eagerly, initialValue = "")

    final override val amountState: Flow<InputState<BigDecimal?>> = inputState
        .map { inputState ->
            inputState.map { input -> input.parseBigDecimalOrNull() }
        }.share()

    private val _amount: Flow<BigDecimal> = amountState
        .map { it.value.orZero() }
        .share()

    @Deprecated("Use amountState instead")
    override val amount: Flow<BigDecimal> = _amount

    override val fiatAmount: Flow<String> = combine(tokenFlow.filterNotNull(), _amount) { token, amount ->
        token.amountToFiat(amount).formatAsCurrency(token.currency)
    }
        .shareInBackground()

    override val backPressuredAmount: Flow<BigDecimal>
        get() = _amount.debounce(DEBOUNCE_DURATION_MILLIS.milliseconds)

    override val maxAction: AmountChooserMixinBase.MaxAction = RealMaxAction()

    private fun String.parseBigDecimalOrNull() = replace(",", "").toBigDecimalOrNull()

    private fun defaultState(): InputState<String> = InputState(value = "", initiatedByUser = true, inputKind = InputKind.REGULAR)

    private fun <T, R> InputState<T>.map(valueTransform: (T) -> R): InputState<R> {
        return InputState(valueTransform(value), initiatedByUser, inputKind)
    }

    private inner class RealMaxAction : AmountChooserMixinBase.MaxAction {

        private var activeDelayedClick: Job? = null

        private val maxAvailableForActionAmount = combine(chainAssetFlow, maxActionProvider.maxAvailableForAction()) { chainAsset, maxAvailableForAction ->
            if (chainAsset == null || maxAvailableForAction == null) {
                null
            } else {
                chainAsset.amountFromPlanks(maxAvailableForAction)
            }
        }.shareInBackground()

        override val display: Flow<String?> = combine(chainAssetFlow, maxActionProvider.maxAvailableForDisplay()) { chainAsset, maxAvailableForDisplay ->
            if (chainAsset == null || maxAvailableForDisplay == null) {
                null
            } else {
                maxAvailableForDisplay.formatPlanks(chainAsset)
            }
        }.inBackground()

        override val maxClick: Flow<MaxClick?> = maxAvailableForActionAmount.map { maxAvailableForAction ->
            getMaxClickAction(maxAvailableForAction)
        }.inBackground()

        init {
            setupAutoUpdates()

            cancelDelayedInputOnInputChange()
        }

        private fun createImmediateMaxClicked(amount: BigDecimal): MaxClick {
            val potentialState = maxAmountInputState(amount)

            return {
                cancelActiveDelayedClick()

                inputState.value = potentialState
            }
        }

        private fun createDelayedMaxClicked(): MaxClick {
            return {
                cancelActiveDelayedClick()

                activeDelayedClick = launch {
                    val amount = maxAvailableForActionAmount.firstNotNull()
                    val potentialState = maxAmountInputState(amount)
                    inputState.value = potentialState
                }
            }
        }

        private fun setupAutoUpdates() {
            maxAvailableForActionAmount
                .filterNotNull()
                .filter { amount ->
                    val currentState = amountState.first()
                    currentState.inputKind == InputKind.MAX_ACTION && amount != currentState.value
                }
                .onEach { newAmount ->
                    inputState.value = maxAmountInputState(newAmount)
                }
                .launchIn(this@BaseAmountChooserProvider)
        }

        private fun getMaxClickAction(maxAvailableForAction: BigDecimal?): MaxClick {
            return if (maxAvailableForAction != null) {
                createImmediateMaxClicked(maxAvailableForAction)
            } else {
                createDelayedMaxClicked()
            }
        }

        private fun cancelDelayedInputOnInputChange() {
            amountState.onEach {
                if (it.inputKind != InputKind.MAX_ACTION) cancelActiveDelayedClick()
            }.launchIn(this@BaseAmountChooserProvider)
        }

        private fun maxAmountInputState(amount: BigDecimal): InputState<String> {
            return InputState(amount.toPlainString(), initiatedByUser = true, inputKind = InputKind.MAX_ACTION)
        }

        private fun MaxActionProvider?.maxAvailableForAction(): Flow<Balance?> = this?.maxAvailableForAction?.balance() ?: flowOf(null)

        private fun MaxActionProvider?.maxAvailableForDisplay(): Flow<Balance?> = this?.maxAvailableForDisplay ?: flowOf(null)

        private fun Flow<MaxAvailableForAction?>.balance(): Flow<Balance?> = map { it?.balance }

        private fun cancelActiveDelayedClick() {
            activeDelayedClick?.cancel()
            activeDelayedClick = null
        }
    }
}
