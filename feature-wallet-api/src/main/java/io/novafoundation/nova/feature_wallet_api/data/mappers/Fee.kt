package io.novafoundation.nova.feature_wallet_api.data.mappers

import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import java.math.BigDecimal

fun mapFeeToFeeModel(
    fee: BigDecimal,
    token: Token,
    includeZeroFiat: Boolean = true
) = FeeModel(
    fee = fee,
    display = mapAmountToAmountModel(fee, token, includeZeroFiat)
)
