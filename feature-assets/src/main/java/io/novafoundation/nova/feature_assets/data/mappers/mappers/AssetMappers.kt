package io.novafoundation.nova.feature_assets.data.mappers.mappers

import io.novafoundation.nova.common.utils.formatting.formatAsChange
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.model.TokenModel
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import java.math.BigDecimal

fun mapTokenToTokenModel(token: Token): TokenModel {
    return with(token) {
        val rateChange = token.recentRateChange

        val changeColorRes = when {
            rateChange == null -> R.color.text_secondary
            rateChange.isNonNegative -> R.color.text_positive
            else -> R.color.text_negative
        }

        TokenModel(
            configuration = configuration,
            rate = (rate ?: BigDecimal.ZERO).formatAsCurrency(token.currency),
            recentRateChange = (recentRateChange ?: BigDecimal.ZERO).formatAsChange(),
            rateChangeColorRes = changeColorRes
        )
    }
}
