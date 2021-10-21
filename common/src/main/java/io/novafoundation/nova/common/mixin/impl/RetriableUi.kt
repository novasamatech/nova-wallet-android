package io.novafoundation.nova.common.mixin.impl

import android.content.Context
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.view.dialog.retryDialog

fun <T> BaseFragment<T>.observeRetries(
    viewModel: T,
    context: Context = requireContext()
) where T : BaseViewModel, T : Retriable {
    viewModel.retryEvent.observeEvent {
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
