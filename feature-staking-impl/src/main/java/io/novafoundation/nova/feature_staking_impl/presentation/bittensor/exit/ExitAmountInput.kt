package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState.InputKind
import java.math.BigDecimal

internal data class ExitAmountInput(
    val text: String,
    val revision: Long = 0,
    val inputKind: InputKind = InputKind.REGULAR
) {

    fun edit(value: String): ExitAmountInput {
        return if (value == text) this else copy(text = value, inputKind = InputKind.REGULAR)
    }

    fun selectMax(amount: BigDecimal): ExitAmountInput {
        return copy(text = amount.max(BigDecimal.ZERO).toStripTrailingZerosString(), inputKind = InputKind.MAX_ACTION)
    }

    fun updateMax(amount: BigDecimal?): ExitAmountInput {
        return if (inputKind == InputKind.MAX_ACTION && amount != null) selectMax(amount) else this
    }
}
