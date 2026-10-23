package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.common.view.dialog.retryDialog
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.coroutines.flow.Flow

fun BaseFragmentMixin<*>.observeBittensorRetries(events: Flow<BittensorReadFailure>) {
    events.observeWhenVisible { error ->
        if (error.retryable) {
            retryDialog(
                context = fragment.requireContext(),
                onRetry = error::onRetry
            ) {
                setTitle(error.title)
                setMessage(error.message)
                // iOS offers Skip beside Retry on a failed read.
                setNegativeButton(R.string.common_skip) { _, _ -> error.onDismiss() }
            }
        } else {
            errorDialog(fragment.requireContext(), onConfirm = error::onDismiss) {
                setTitle(error.title)
                setMessage(error.message)
            }
        }
    }
}
