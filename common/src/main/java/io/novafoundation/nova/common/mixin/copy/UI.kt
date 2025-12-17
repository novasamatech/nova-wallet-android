package io.novafoundation.nova.common.mixin.copy

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.CopierBottomSheet

fun <T> BaseFragment<T, *>.setupCopyText(viewModel: T) where T : BaseViewModel, T : CopyTextLauncher {
    viewModel.showCopyTextDialog.observeEvent {
        CopierBottomSheet(
            requireContext(),
            payload = it
        ).show()
    }
}
