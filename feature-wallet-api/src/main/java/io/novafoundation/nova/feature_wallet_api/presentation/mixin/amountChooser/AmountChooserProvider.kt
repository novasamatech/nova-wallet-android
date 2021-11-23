package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import java.math.BigDecimal
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

private const val DEBOUNCE_DURATION_MILLIS = 500

class AmountChooserProviderFactory : AmountChooserMixin.Factory {

    override fun create(
        scope: CoroutineScope,
        assetFlow: Flow<Asset>,
        assetUiMapper: AssetUiMapper,
    ): AmountChooserMixin.Presentation {
        return AmountChooserProvider(
            coroutineScope = scope,
            assetFlow = assetFlow,
            assetUiMapper = assetUiMapper
        )
    }
}

@OptIn(ExperimentalTime::class)
class AmountChooserProvider(
    coroutineScope: CoroutineScope,
    assetFlow: Flow<Asset>,
    private val assetUiMapper: AssetUiMapper,
) : AmountChooserMixin.Presentation,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val amountInput: MutableStateFlow<String> = MutableStateFlow("")

    override val assetModel = assetFlow
        .map { assetUiMapper(it) }
        .inBackground()
        .share()

    override val amount: Flow<BigDecimal> = amountInput
        .mapNotNull { it.toBigDecimalOrNull() }
        .onStart { emit(BigDecimal.ZERO) }
        .share()

    override val backPressuredAmount: Flow<BigDecimal> = amount
        .debounce(DEBOUNCE_DURATION_MILLIS.milliseconds)

    override val fiatAmount: Flow<String> = assetFlow.combine(amount) { asset, amount ->
        asset.token.fiatAmount(amount).formatAsCurrency()
    }
        .inBackground()
        .share()
}
