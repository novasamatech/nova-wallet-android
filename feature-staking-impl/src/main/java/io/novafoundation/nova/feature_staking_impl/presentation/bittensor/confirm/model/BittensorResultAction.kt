package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model

import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult

/** A result sheet or page button, as iOS `SubtensorResultAction`. */
enum class BittensorResultAction {
    DONE,
    TRY_AGAIN,
    CLOSE,
    VIEW_POSITION,
    DISCOVER_SUBNETS,
    YOUR_BITTENSOR,
    BACK_TO_POSITION
}

/**
 * Whether Try again can succeed after a failed submission, as iOS `SubtensorSubmissionFailure.isRetryable`: a missing
 * subnet, a disabled subnet token, an unregistered hotkey, a coldkey swap, safe mode, the staking hotkey cap and a too
 * heavy root claim do not change on a retry.
 */
val BittensorOperationResult.Failure.isRetryable: Boolean
    get() {
        val error = cause as? DispatchError.Module ?: return true

        return (error.module.name to error.error.name) !in NON_RETRYABLE_DISPATCH_ERRORS
    }

private val NON_RETRYABLE_DISPATCH_ERRORS = setOf(
    "SubtensorModule" to "SubnetNotExists",
    "SubtensorModule" to "SubtokenDisabled",
    "Swap" to "SubtokenDisabled",
    "SubtensorModule" to "HotKeyAccountNotExists",
    "SubtensorModule" to "ColdkeySwapAnnounced",
    "SubtensorModule" to "ColdkeySwapDisputed",
    "SubtensorModule" to "TooManyStakingHotkeys",
    "SubtensorModule" to "RootClaimTooHeavy",
    "System" to "CallFiltered"
)
