package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import androidx.annotation.StringRes
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import java.math.BigInteger

interface AmountChooserMixinBase : CoroutineScope {

    val inputState: MutableStateFlow<InputState<String>>

    @Deprecated("Use amountInput instead")
    val amountInput: StateFlow<String>

    interface Presentation : AmountChooserMixinBase {

        @Deprecated("Use amountState instead")
        val amount: Flow<BigDecimal>

        val amountState: Flow<InputState<BigDecimal?>>

        val backPressuredAmount: Flow<BigDecimal>
    }

    interface Factory {

        fun create(scope: CoroutineScope)
    }

    class InputState<T>(val value: T, val initiatedByUser: Boolean)
}

interface AmountChooserMixin : AmountChooserMixinBase {

    val usedAssetFlow: Flow<Asset>

    val assetModel: Flow<ChooseAmountModel>

    val fiatAmount: Flow<String>

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
    inputState.value = InputState(value = amount.toPlainString(), initiatedByUser = false)
}

fun AmountChooserMixin.Presentation.setAmountInput(amountInput: String) {
    inputState.value = InputState(value = amountInput, initiatedByUser = false)
}
