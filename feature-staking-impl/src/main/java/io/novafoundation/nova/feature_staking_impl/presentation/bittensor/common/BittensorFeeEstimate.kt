package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus

class BittensorFeeEstimate<R>(val request: R, val fee: Fee) : Fee by fee

internal fun <R, D> FeeStatus<BittensorFeeEstimate<R>, D>.forRequest(request: R?): FeeStatus<BittensorFeeEstimate<R>, D> {
    return when {
        request == null -> FeeStatus.NoFee
        this is FeeStatus.Loaded && feeModel.fee.request != request -> FeeStatus.Loading(visibleDuringProgress = true)
        else -> this
    }
}
