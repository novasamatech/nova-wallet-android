package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import androidx.annotation.StringRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

private const val DEBOUNCE_DURATION_MILLIS = 500

class AmountChooserProviderFactory(
    private val resourceManager: ResourceManager
) : AmountChooserMixin.Factory {

    override fun create(
        scope: CoroutineScope,
        assetFlow: Flow<Asset>,
        availableBalanceFlow: Flow<BigInteger>,
        balanceLabel: Int?
    ): AmountChooserProvider {
        return AmountChooserProvider(
            coroutineScope = scope,
            usedAssetFlow = assetFlow,
            availableBalanceFlow = availableBalanceFlow,
            balanceLabel = balanceLabel,
            resourceManager = resourceManager
        )
    }

    override fun create(
        scope: CoroutineScope,
        assetFlow: Flow<Asset>,
        balanceField: (Asset) -> BigDecimal,
        @StringRes balanceLabel: Int?
    ): AmountChooserMixin.Presentation {
        return create(
            scope = scope,
            assetFlow = assetFlow,
            availableBalanceFlow = assetFlow.map { it.token.planksFromAmount(balanceField(it)) },
            balanceLabel = balanceLabel,
        )
    }
}

@OptIn(ExperimentalTime::class)
class AmountChooserProvider(
    coroutineScope: CoroutineScope,
    override val usedAssetFlow: Flow<Asset>,
    private val resourceManager: ResourceManager,
    private val availableBalanceFlow: Flow<BigInteger>,
    @StringRes private val balanceLabel: Int?
) : AmountChooserMixin.Presentation,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val amountInput: MutableStateFlow<String> = MutableStateFlow("")

    override val assetModel = combine(
        availableBalanceFlow.onStart<BigInteger?> { emit(null) },
        usedAssetFlow
    ) { balance, asset ->
        ChooseAmountModel(asset, resourceManager, balance, balanceLabel)
    }
        .shareInBackground()

    override val amount: Flow<BigDecimal> = amountInput
        .map { it.toBigDecimalOrNull() ?: BigDecimal.ZERO }
        .share()

    override val backPressuredAmount: Flow<BigDecimal> = amount
        .debounce(DEBOUNCE_DURATION_MILLIS.milliseconds)

    override val fiatAmount: Flow<String> = usedAssetFlow.combine(amount) { asset, amount ->
        asset.token.amountToFiat(amount).formatAsCurrency(asset.token.currency)
    }
        .inBackground()
        .share()
}
