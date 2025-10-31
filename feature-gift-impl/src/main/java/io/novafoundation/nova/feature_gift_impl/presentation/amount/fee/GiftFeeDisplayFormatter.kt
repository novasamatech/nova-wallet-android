package io.novafoundation.nova.feature_gift_impl.presentation.amount.fee

import io.novafoundation.nova.feature_gift_impl.domain.models.GiftFee
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.FeeFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.toFeeDisplay

class GiftFeeDisplayFormatter(
    private val amountFormatter: AmountFormatter
) : FeeFormatter<GiftFee, FeeDisplay> {

    override suspend fun formatFee(
        fee: GiftFee,
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
