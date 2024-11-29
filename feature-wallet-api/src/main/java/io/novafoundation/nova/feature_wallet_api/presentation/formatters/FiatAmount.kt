package io.novafoundation.nova.feature_wallet_api.presentation.formatters

import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.FiatAmount

fun FiatAmount.formatAsCurrency() = price.formatAsCurrency(currency)
