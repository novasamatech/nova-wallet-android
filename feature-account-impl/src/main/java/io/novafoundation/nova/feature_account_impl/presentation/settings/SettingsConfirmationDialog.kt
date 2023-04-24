package io.novafoundation.nova.feature_account_impl.presentation.settings

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationAwaitable
import io.novafoundation.nova.common.view.dialog.dialog
import io.novafoundation.nova.feature_account_impl.R

class SettingsConfirmationData(val title: Int, val message: Int)

fun BaseFragment<*>.setupSettingsConfirmationDialog(awaitableMixin: ConfirmationAwaitable<SettingsConfirmationData>) {
    awaitableMixin.awaitableActionLiveData.observeEvent {
        dialog(requireContext(), R.style.AccentPositiveAlertDialogTheme) {
            setTitle(it.payload.title)
            setMessage(it.payload.message)
            setPositiveButton(R.string.common_enable) { _, _ -> it.onSuccess(Unit) }
            setNegativeButton(R.string.common_cancel) { _, _ -> it.onCancel() }
        }
    }
}
