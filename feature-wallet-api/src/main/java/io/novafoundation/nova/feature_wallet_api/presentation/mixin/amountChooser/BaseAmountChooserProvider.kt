package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.firstNotNull
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.mapNullable
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState.InputKind
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxAvailableBalance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.actualAmount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
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
    private val maxActionProvider: MaxActionProvider?,
    fiatFormatter: AmountChooserMixinBase.FiatFormatter = DefaultFiatFormatter(),
    private val fieldValidator: FieldValidator? = null,
) : AmountChooserMixinBase.Presentation,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

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

    @Suppress("DEPRECATION")
    override val fieldError: Flow<FieldValidationResult> = fieldValidator?.observe(amountInput)
        ?: flowOf(FieldValidationResult.Ok)

    final override val amountState: Flow<InputState<BigDecimal?>> = inputState
        .map { inputState ->
            inputState.map { input -> input.parseBigDecimalOrNull() }
        }.share()

    private val _amount: Flow<BigDecimal> = amountState
        .map { it.value.orZero() }
        .share()

    @Deprecated("Use amountState instead")
    override val amount: Flow<BigDecimal> = _amount

    override val fiatAmount: Flow<CharSequence> = fiatFormatter.formatFlow(tokenFlow.filterNotNull(), _amount)
        .shareInBackground()

    override val backPressuredAmount: Flow<BigDecimal>
        get() = _amount.debounce(DEBOUNCE_DURATION_MILLIS.milliseconds)

    override val maxAction: AmountChooserMixinBase.MaxAction = RealMaxAction()

    override val requestFocusLiveData: MutableLiveData<Event<Unit>> = MutableLiveData()

    private fun String.parseBigDecimalOrNull(): BigDecimal? {
        if (isEmpty()) return null

        return replace(",", "").toBigDecimalOrNull()
    }

    private fun defaultState(): InputState<String> = InputState(value = "", initiatedByUser = true, inputKind = InputKind.REGULAR)

    private fun <T, R> InputState<T>.map(valueTransform: (T) -> R): InputState<R> {
        return InputState(valueTransform(value), initiatedByUser, inputKind)
    }

    private inner class RealMaxAction : AmountChooserMixinBase.MaxAction {

        private var activeDelayedClick: Job? = null

        private val maxAvailableBalance = maxActionProvider.maxAvailableBalanceOrNull()
            .shareInBackground()

        private val maxAvailableForActionAmount = maxAvailableBalance.mapNullable { maxAvailableBalance ->
            maxAvailableBalance.actualAmount
        }.shareInBackground()

        override val display: Flow<String?> = maxAvailableBalance.mapNullable { maxAvailableBalance ->
            maxAvailableBalance.displayedBalance.formatPlanks(maxAvailableBalance.chainAsset)
        }.inBackground()

        override val maxClick: Flow<MaxClick> = maxAvailableForActionAmount.map { maxAvailableForAction ->
            getMaxClickAction(maxAvailableForAction)
        }.shareInBackground()

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
            return InputState(amount.toStripTrailingZerosString(), initiatedByUser = true, inputKind = InputKind.MAX_ACTION)
        }

        private fun MaxActionProvider?.maxAvailableBalanceOrNull(): Flow<MaxAvailableBalance?> {
            if (this == null) return flowOf(null)

            return maxAvailableBalance
                .onStart<MaxAvailableBalance?> { emit(null) }
        }

        private fun cancelActiveDelayedClick() {
            activeDelayedClick?.cancel()
            activeDelayedClick = null
        }
    }
}
