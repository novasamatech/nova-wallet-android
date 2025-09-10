package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter

import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.toFeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig

class DefaultFeeFormatter<F : FeeBase>(
    private val amountFormatter: AmountFormatter
) : FeeFormatter<F, FeeDisplay> {

    override suspend fun formatFee(
        fee: F,
        configuration: FeeFormatter.Configuration,
        context: FeeFormatter.Context
    ): FeeDisplay {
        return amountFormatter.formatAmountToAmountModel(
            amountInPlanks = fee.amount,
            token = context.token(fee.asset),
            AmountConfig(includeZeroFiat = configuration.showZeroFiat)
        ).toFeeDisplay()
    }

    override suspend fun createLoadingStatus(): FeeStatus.Loading {
        return FeeStatus.Loading(visibleDuringProgress = true)
    }
}
