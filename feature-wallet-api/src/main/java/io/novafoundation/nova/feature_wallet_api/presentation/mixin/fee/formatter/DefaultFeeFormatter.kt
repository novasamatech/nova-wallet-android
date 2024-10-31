package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter

import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.toFeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel

class DefaultFeeFormatter<F : FeeBase> : FeeFormatter<F, FeeDisplay> {

    override suspend fun formatFee(
        fee: F,
        configuration: FeeFormatter.Configuration,
        context: FeeFormatter.Context
    ): FeeDisplay {
        return mapAmountToAmountModel(
            amountInPlanks = fee.amount,
            token = context.feeToken(),
            includeZeroFiat = configuration.showZeroFiat
        ).toFeeDisplay()
    }

    override suspend fun createLoadingStatus(): FeeStatus.Loading {
        return FeeStatus.Loading(visibleDuringProgress = true)
    }
}
