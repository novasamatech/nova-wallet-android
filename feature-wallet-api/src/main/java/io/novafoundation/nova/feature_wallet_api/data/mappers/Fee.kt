package io.novafoundation.nova.feature_wallet_api.data.mappers

import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.FeeModel
import java.math.BigDecimal

fun mapFeeToFeeModel(
    fee: BigDecimal,
    token: Token
) = FeeModel(
    fee = fee,
    displayToken = fee.formatTokenAmount(token.configuration),
    displayFiat = token.fiatAmount(fee).formatAsCurrency()
)
