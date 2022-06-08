package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import androidx.annotation.StringRes
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal
import java.math.BigInteger

interface AmountChooserMixin : CoroutineScope {

    val usedAssetFlow: Flow<Asset>

    val amountInput: MutableStateFlow<String>

    val assetModel: Flow<ChooseAmountModel>

    val fiatAmount: Flow<String>

    interface Presentation : AmountChooserMixin {

        val amount: Flow<BigDecimal>

        val backPressuredAmount: Flow<BigDecimal>
    }

    interface Factory {

        fun create(
            scope: CoroutineScope,
            assetFlow: Flow<Asset>,
            availableBalanceFlow: Flow<BigInteger>,
            @StringRes balanceLabel: Int?,
        ): AmountChooserProvider

        fun create(
            scope: CoroutineScope,
            assetFlow: Flow<Asset>,
            balanceField: (Asset) -> BigDecimal,
            @StringRes balanceLabel: Int?
        ): Presentation
    }
}

fun AmountChooserMixin.Presentation.setAmount(amount: BigDecimal) {
    amountInput.value = amount.toPlainString()
}
