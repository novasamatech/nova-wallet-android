package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import kotlin.time.Duration.Companion.milliseconds

private const val DEBOUNCE_DURATION_MILLIS = 500

open class BaseAmountChooserProvider(
    coroutineScope: CoroutineScope,
) : AmountChooserMixinBase.Presentation,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    final override val inputState = MutableStateFlow(defaultState())

    final override val amountInput = inputState.map { it.value }
        .stateIn(this, SharingStarted.Eagerly, initialValue = "")

    final override val amountState: Flow<InputState<BigDecimal?>> = inputState
        .map { inputState ->
            inputState.map { input -> input.parseBigDecimalOrNull() }
        }.share()

    override val amount: Flow<BigDecimal> = amountState
        .map { it.value.orZero() }
        .share()

    override val backPressuredAmount: Flow<BigDecimal>
        get() = amount.debounce(DEBOUNCE_DURATION_MILLIS.milliseconds)

    private fun String.parseBigDecimalOrNull() = replace(",", "").toBigDecimalOrNull()

    private fun defaultState(): InputState<String> = InputState(value = "", initiatedByUser = true)

    private fun <T, R> InputState<T>.map(valueTransform: (T) -> R): InputState<R> {
        return InputState(valueTransform(value), initiatedByUser)
    }
}
