package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import androidx.core.view.isVisible
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.view.FeeView

internal fun FeeView.setBittensorFeeStatus(status: FeeStatus<*, FeeDisplay>) {
    when (status) {
        FeeStatus.NoFee -> {
            isVisible = true
            showValue("—")
        }
        is FeeStatus.Loading -> setFeeStatus(FeeStatus.Loading(visibleDuringProgress = true))
        else -> setFeeStatus(status)
    }
}
