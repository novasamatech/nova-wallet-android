package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.FiatConfig
import java.math.BigDecimal

interface GenericFiatFormatter<T> {

    fun formatFiat(fiatAmount: BigDecimal, currency: Currency, config: FiatConfig = FiatConfig()): T
}
