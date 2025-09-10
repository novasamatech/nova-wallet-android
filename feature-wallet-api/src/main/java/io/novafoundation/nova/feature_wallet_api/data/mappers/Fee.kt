package io.novafoundation.nova.feature_wallet_api.data.mappers

import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.toFeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("This is a internal logic related to fee mixin. To access or set the fee use corresponding methods from FeeLoaderMixinV2.Presentation")
fun <F : FeeBase> mapFeeToFeeModel(
    fee: F,
    token: Token,
    includeZeroFiat: Boolean = true,
    amountFormatter: AmountFormatter
): FeeModel<F, FeeDisplay> = FeeModel(
    display = amountFormatter.formatAmountToAmountModel(
        amountInPlanks = fee.amount,
        token = token,
        AmountConfig(includeZeroFiat = includeZeroFiat)
    ).toFeeDisplay(),
    fee = fee
)
