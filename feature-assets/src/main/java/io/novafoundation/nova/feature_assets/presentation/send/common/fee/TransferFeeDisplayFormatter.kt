package io.novafoundation.nova.feature_assets.presentation.send.common.fee

import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_assets.domain.send.model.TransferFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.DefaultFeeFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.FeeFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus

class TransferFeeDisplayFormatter(
    var crossChainFeeShown: Boolean = false,

    private val componentDelegate: FeeFormatter<FeeBase, FeeDisplay> = DefaultFeeFormatter()
) : FeeFormatter<TransferFee, TransferFeeDisplay> {

    override suspend fun formatFee(
        fee: TransferFee,
        configuration: FeeFormatter.Configuration,
        context: FeeFormatter.Context
    ): TransferFeeDisplay {
        return TransferFeeDisplay(
            originFee = componentDelegate.formatFee(fee.originFee.totalInSubmissionAsset, configuration, context),
            crossChainFee = fee.crossChainFee?.let { componentDelegate.formatFee(it, configuration, context) }
        )
    }

    override suspend fun createLoadingStatus(): FeeStatus.Loading {
        return FeeStatus.Loading(visibleDuringProgress = crossChainFeeShown)
    }
}
