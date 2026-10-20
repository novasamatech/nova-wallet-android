package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import java.math.BigDecimal

internal enum class ExitContinuationDecision {
    CANCEL, CONTINUE, REVALIDATE
}

internal fun exitContinuationDecision(
    original: ExitAmountInput,
    current: ExitAmountInput,
    originalSlippage: BigDecimal,
    currentSlippage: BigDecimal,
    requestCurrent: Boolean
): ExitContinuationDecision = when {
    !original.hasSameIntent(current) || originalSlippage != currentSlippage -> ExitContinuationDecision.CANCEL
    requestCurrent -> ExitContinuationDecision.CONTINUE
    else -> ExitContinuationDecision.REVALIDATE
}
