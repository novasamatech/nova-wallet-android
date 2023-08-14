package io.novafoundation.nova.feature_wallet_api.data.mappers

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.InlineFee
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import java.math.BigDecimal

fun mapFeeToFeeModel(
    fee: Fee,
    token: Token,
    includeZeroFiat: Boolean = true
) = FeeModel(
    decimalFee = DecimalFee(
        fee = fee,
        decimalAmount = token.amountFromPlanks(fee.amount)
    ),
    display = mapAmountToAmountModel(
        amountInPlanks = fee.amount,
        token = token,
        includeZeroFiat = includeZeroFiat
    )
)

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Backward-compatible adapter")
fun mapFeeToFeeModel(
    feeAmount: BigDecimal,
    token: Token,
    includeZeroFiat: Boolean = true
) : FeeModel {
    return mapFeeToFeeModel(InlineFee(token.planksFromAmount(feeAmount)), token, includeZeroFiat)
}
