package io.novafoundation.nova.common.mixin.copy

import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.CopierBottomSheet

fun <T> BaseFragment<T, *>.setupCopyText(viewModel: T) where T : BaseViewModel, T : CopyTextMixin {
    viewModel.showCopyTextDialog.observeEvent {
        CopierBottomSheet(
            requireContext(),
            value = it,
            buttonNameRes = R.string.common_copy_hash
        ).show()
    }
}
