package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import java.math.BigDecimal

class PricedAmount(
    val amount: BigDecimal,
    val price: BigDecimal,
    val currency: Currency
)
