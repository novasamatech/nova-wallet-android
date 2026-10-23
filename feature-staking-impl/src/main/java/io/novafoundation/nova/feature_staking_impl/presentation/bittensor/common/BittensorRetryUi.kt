package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.common.view.dialog.retryDialog
import kotlinx.coroutines.flow.Flow

fun BaseFragmentMixin<*>.observeBittensorRetries(events: Flow<BittensorReadFailure>) {
    events.observeWhenVisible { error ->
        if (error.retryable) {
            retryDialog(
                context = fragment.requireContext(),
                onRetry = error.onRetry
            ) {
                setTitle(error.title)
                setMessage(error.message)
            }
        } else {
            errorDialog(fragment.requireContext()) {
                setTitle(error.title)
                setMessage(error.message)
            }
        }
    }
}
