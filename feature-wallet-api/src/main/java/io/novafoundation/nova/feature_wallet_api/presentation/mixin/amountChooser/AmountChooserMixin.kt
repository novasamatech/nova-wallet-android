package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState.InputKind
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

typealias MaxClick = () -> Unit

interface AmountChooserMixinBase : CoroutineScope {

    val fiatAmount: Flow<CharSequence>

    val inputState: MutableStateFlow<InputState<String>>

    @Deprecated(
        message = "Use `inputState` instead",
        replaceWith = ReplaceWith(
            expression = "inputState.map { it.value }",
            imports = ["kotlinx.coroutines.flow.map"]
        )
    )
    val amountInput: StateFlow<String>

    val fieldError: Flow<FieldValidationResult>

    val maxAction: MaxAction

    val requestFocusLiveData: MutableLiveData<Event<Unit>>

    interface Presentation : AmountChooserMixinBase {

        val amount: Flow<BigDecimal>

        val amountState: Flow<InputState<BigDecimal?>>

        val backPressuredAmount: Flow<BigDecimal>
    }

    interface FiatFormatter {

        fun formatFlow(tokenFlow: Flow<Token>, amountFlow: Flow<BigDecimal>): Flow<CharSequence>
    }

    data class InputState<T>(
        val value: T,
        // TODO revisit this flag. Seems to only be used in SwapMainSettingsViewModel and its purpose and semantics looks unclear
        val initiatedByUser: Boolean,
        val inputKind: InputKind
    ) {

        enum class InputKind {
            REGULAR, MAX_ACTION, BLOCKED
        }
    }

    interface MaxAction {

        val display: Flow<String?>

        val maxClick: Flow<MaxClick?>
    }
}

interface AmountChooserMixin : AmountChooserMixinBase {

    val usedAssetFlow: Flow<Asset>

    val assetModel: Flow<ChooseAmountModel>

    interface Presentation : AmountChooserMixin, AmountChooserMixinBase.Presentation

    interface Factory {

        fun create(
            scope: CoroutineScope,
            assetFlow: Flow<Asset>,
            maxActionProvider: MaxActionProvider?,
        ): Presentation

        fun create(
            scope: CoroutineScope,
            assetFlow: Flow<Asset>,
            balanceField: (Asset) -> BigDecimal,
            maxActionProvider: MaxActionProvider?,
        ): Presentation
    }
}

fun AmountChooserMixinBase.Presentation.setAmount(amount: BigDecimal, initiatedByUser: Boolean = false) {
    inputState.value = InputState(value = amount.toStripTrailingZerosString(), initiatedByUser, inputKind = InputKind.REGULAR)
}

fun AmountChooserMixinBase.Presentation.setAmountInput(amountInput: String, initiatedByUser: Boolean = false) {
    inputState.value = InputState(value = amountInput, initiatedByUser, inputKind = InputKind.REGULAR)
}

fun AmountChooserMixinBase.Presentation.setInputBlocked() {
    inputState.value = inputState.value.copy(inputKind = InputKind.BLOCKED)
}

fun AmountChooserMixinBase.Presentation.setBlockedAmount(amount: BigDecimal) {
    inputState.value = InputState(value = amount.toStripTrailingZerosString(), initiatedByUser = false, inputKind = InputKind.BLOCKED)
}

suspend fun AmountChooserMixinBase.Presentation.invokeMaxClick() {
    maxAction.maxClick.first()?.invoke()
}

fun InputKind.isInputBlocked(): Boolean {
    return this == InputKind.BLOCKED
}

fun InputKind.isInputAllowed(): Boolean {
    return !isInputBlocked()
}
