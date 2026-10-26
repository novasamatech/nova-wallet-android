package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.common.view.dialog.retryDialog
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.coroutines.flow.Flow

/** [onSkip] runs when the user leaves a failure without retrying it, e.g. to close a screen that cannot show without the read. */
fun BaseFragmentMixin<*>.observeBittensorRetries(events: Flow<BittensorReadFailure>, onSkip: () -> Unit = {}) {
    events.observeWhenVisible { error ->
        val skip = {
            error.onDismiss()
            onSkip()
        }
        if (error.retryable) {
            retryDialog(
                context = fragment.requireContext(),
                onRetry = error::onRetry
            ) {
                setTitle(error.title)
                setMessage(error.message)
                setNegativeButton(R.string.common_skip) { _, _ -> skip() }
            }
        } else {
            errorDialog(fragment.requireContext(), onConfirm = skip) {
                setTitle(error.title)
                setMessage(error.message)
            }
        }
    }
}
