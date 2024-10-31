package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import java.math.BigDecimal

class FiatAmount(
    val currency: Currency,
    val price: BigDecimal
)
