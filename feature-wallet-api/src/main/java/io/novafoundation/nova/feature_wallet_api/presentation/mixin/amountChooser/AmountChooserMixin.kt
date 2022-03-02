package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetSelectorModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal

typealias AssetUiMapper = (Asset) -> AssetSelectorModel

interface AmountChooserMixin {

    val amountInput: MutableStateFlow<String>

    val assetModel: Flow<AssetSelectorModel>

    val fiatAmount: Flow<String>

    interface Presentation : AmountChooserMixin {

        val amount: Flow<BigDecimal>

        val backPressuredAmount: Flow<BigDecimal>
    }

    interface Factory {

        fun create(
            scope: CoroutineScope,
            assetFlow: Flow<Asset>,
            assetUiMapper: AssetUiMapper,
        ): Presentation
    }
}

fun AmountChooserMixin.Presentation.setAmount(amount: BigDecimal) {
    amountInput.value = amount.toPlainString()
}
