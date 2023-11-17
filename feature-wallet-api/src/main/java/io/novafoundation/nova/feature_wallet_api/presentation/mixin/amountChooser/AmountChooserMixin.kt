package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState.InputKind
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.first

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

    val fieldError: Flow<AmountErrorState>

    val maxAction: MaxAction

    val requestFocusLiveData: MutableLiveData<Event<Unit>>

    interface Presentation : AmountChooserMixinBase {

        @Deprecated("Use amountState instead")
        val amount: Flow<BigDecimal>

        val amountState: Flow<InputState<BigDecimal?>>

        val backPressuredAmount: Flow<BigDecimal>
    }

    interface FiatFormatter {

        fun formatFlow(tokenFlow: Flow<Token>, amountFlow: Flow<BigDecimal>): Flow<CharSequence>
    }

    sealed class AmountErrorState {
        object Valid : AmountErrorState()

        class Invalid(val message: String) : AmountErrorState()
    }

    class InputState<T>(val value: T, val initiatedByUser: Boolean, val inputKind: InputKind) {

        enum class InputKind {
            REGULAR, MAX_ACTION
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
            availableBalanceFlow: Flow<BigInteger>,
            @StringRes balanceLabel: Int?,
        ): Presentation

        fun create(
            scope: CoroutineScope,
            assetFlow: Flow<Asset>,
            balanceField: (Asset) -> BigDecimal,
            @StringRes balanceLabel: Int?
        ): Presentation
    }
}

fun AmountChooserMixinBase.AmountErrorState.getMessageOrNull(): String? {
    return if (this is AmountChooserMixinBase.AmountErrorState.Invalid) {
        message
    } else {
        null
    }
}

fun AmountChooserMixinBase.Presentation.setAmount(amount: BigDecimal, initiatedByUser: Boolean = false) {
    inputState.value = InputState(value = amount.toStripTrailingZerosString(), initiatedByUser, inputKind = InputKind.REGULAR)
}

fun AmountChooserMixinBase.Presentation.setAmountInput(amountInput: String, initiatedByUser: Boolean = false) {
    inputState.value = InputState(value = amountInput, initiatedByUser, inputKind = InputKind.REGULAR)
}

suspend fun AmountChooserMixinBase.Presentation.invokeMaxClick() {
    maxAction.maxClick.first()?.invoke()
}
