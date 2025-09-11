package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.FiatConfig
import java.math.BigDecimal
import java.math.RoundingMode

interface GenericFiatFormatter<T> {

    fun formatFiatNoAbbreviation(amount: BigDecimal, currency: Currency, config: FiatConfig = FiatConfig()): T

    fun formatAsCurrency(amount: BigDecimal, currency: Currency, roundingMode: RoundingMode = RoundingMode.FLOOR, config: FiatConfig = FiatConfig()): T

    fun simpleFormatAsCurrency(amount: BigDecimal, currency: Currency, roundingMode: RoundingMode = RoundingMode.FLOOR, config: FiatConfig = FiatConfig()): T
}
