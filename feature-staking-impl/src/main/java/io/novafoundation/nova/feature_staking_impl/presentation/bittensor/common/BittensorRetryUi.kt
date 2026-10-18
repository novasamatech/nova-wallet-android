package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.view.dialog.retryDialog
import kotlinx.coroutines.flow.Flow

fun BaseFragmentMixin<*>.observeBittensorRetries(events: Flow<RetryPayload>) {
    events.observeWhenVisible { error ->
        retryDialog(
            context = fragment.requireContext(),
            onRetry = error.onRetry,
            onCancel = error.onCancel
        ) {
            setTitle(error.title)
            setMessage(error.message)
        }
    }
}
