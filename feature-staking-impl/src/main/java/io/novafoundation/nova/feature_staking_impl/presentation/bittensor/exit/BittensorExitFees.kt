package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull

internal suspend fun BittensorExitInteractor.feeEstimate(request: BittensorExitRequest?): BittensorFeeEstimate<BittensorExitRequest>? {
    return request?.let { BittensorFeeEstimate(it, estimateFee(it.position, it.quote)) }
}

/**
 * The draft of [quote] once the fee of [feeRequest] has loaded; none after a failed fee or without a quote. The quote may be a
 * block re-quote of the request: the fee stays with the request it was estimated for.
 */
internal fun FeeStatus<BittensorFeeEstimate<BittensorExitRequest>, *>.exitDraft(
    interactor: BittensorExitInteractor,
    feeRequest: BittensorExitRequest,
    quote: BittensorExitQuote?
): ExtendedLoadingState<BittensorExitDraft?> {
    val estimate = loadedFeeOrNull()
    return when {
        this is FeeStatus.Error || this is FeeStatus.NoFee -> ExtendedLoadingState.Loaded(null)
        estimate == null || estimate.request != feeRequest -> ExtendedLoadingState.Loading
        quote == null -> ExtendedLoadingState.Loaded(null)
        else -> ExtendedLoadingState.Loaded(interactor.prepare(feeRequest.position, quote, estimate.fee))
    }
}
