package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import androidx.annotation.StringRes
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

    val maxAction: MaxAction

    interface Presentation : AmountChooserMixinBase {

        @Deprecated("Use amountState instead")
        val amount: Flow<BigDecimal>

        val amountState: Flow<InputState<BigDecimal?>>

        val backPressuredAmount: Flow<BigDecimal>
    }

    interface FiatFormatter {

        fun formatFlow(tokenFlow: Flow<Token>, amountFlow: Flow<BigDecimal>): Flow<CharSequence>
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

fun AmountChooserMixin.Presentation.setAmount(amount: BigDecimal) {
    inputState.value = InputState(value = amount.toPlainString(), initiatedByUser = false, inputKind = InputKind.REGULAR)
}

fun AmountChooserMixin.Presentation.setAmountInput(amountInput: String) {
    inputState.value = InputState(value = amountInput, initiatedByUser = false, inputKind = InputKind.REGULAR)
}
