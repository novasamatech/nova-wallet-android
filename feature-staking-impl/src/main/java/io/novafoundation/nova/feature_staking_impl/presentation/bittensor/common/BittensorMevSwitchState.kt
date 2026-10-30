package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevReviewFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull

data class BittensorMevFeeRequest<R>(val request: R, val enabled: Boolean)

enum class BittensorMevSwitchState(val checked: Boolean, val enabled: Boolean) {
    HIDDEN(false, false), LOADING(true, true), AVAILABLE_ON(true, true), AVAILABLE_OFF(false, true), UNAVAILABLE(false, false)
}

fun bittensorMevSwitchState(supported: Boolean, requested: Boolean, fee: Fee?): BittensorMevSwitchState {
    if (!supported) return BittensorMevSwitchState.HIDDEN
    if (!requested) return BittensorMevSwitchState.AVAILABLE_OFF
    val estimate = fee as? BittensorMevReviewFee ?: return BittensorMevSwitchState.LOADING
    return if (estimate.isProtected) BittensorMevSwitchState.AVAILABLE_ON else BittensorMevSwitchState.UNAVAILABLE
}

internal fun <R, D> FeeStatus<BittensorFeeEstimate<R>, D>.forMevSelection(enabled: Boolean): FeeStatus<BittensorFeeEstimate<R>, D> {
    val selectedFee = loadedFeeOrNull()?.fee as? BittensorMevReviewFee ?: return this
    return if (selectedFee.requested == enabled) this else FeeStatus.Loading(visibleDuringProgress = true)
}
