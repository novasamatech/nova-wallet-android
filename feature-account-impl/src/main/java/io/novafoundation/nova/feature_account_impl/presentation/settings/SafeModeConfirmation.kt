package io.novafoundation.nova.feature_account_impl.presentation.settings

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationAwaitable
import io.novafoundation.nova.common.view.dialog.dialog
import io.novafoundation.nova.feature_account_impl.R

fun BaseFragment<*>.setupSafeModeConfirmation(awaitableMixin: ConfirmationAwaitable<Unit>) {
    awaitableMixin.awaitableActionLiveData.observeEvent {
        dialog(requireContext(), R.style.AccentPositiveAlertDialogTheme) {
            setTitle(R.string.settings_safe_mode_confirmation_title)
            setMessage(getString(R.string.settings_safe_mode_confirmation_message))
            setPositiveButton(R.string.common_enable) { _, _ -> it.onSuccess(Unit) }
            setNegativeButton(R.string.common_cancel) { _, _ -> it.onCancel() }
        }
    }
}
