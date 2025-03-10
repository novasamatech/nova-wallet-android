package io.novafoundation.nova.common.mixin.impl

import android.content.Context
import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.view.dialog.retryDialog

fun BaseFragmentMixin<*>.observeRetries(
    retriable: Retriable,
    context: Context = fragment.requireContext(),
) {
    with(retriable) {
        retryEvent.observeEvent {
            retryDialog(
                context = context,
                onRetry = it.onRetry,
                onCancel = it.onCancel
            ) {
                setTitle(it.title)
                setMessage(it.message)
            }
        }
    }
}

fun <T> BaseFragmentMixin<T>.observeRetries(
    viewModel: T,
    context: Context = fragment.requireContext(),
) where T : BaseViewModel, T : Retriable {
    observeRetries(retriable = viewModel, context)
}
