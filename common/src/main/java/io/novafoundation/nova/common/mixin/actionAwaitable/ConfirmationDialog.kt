package io.novafoundation.nova.common.mixin.actionAwaitable

import androidx.annotation.StyleRes
import androidx.core.content.ContentProviderCompat.requireContext
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.view.dialog.dialog

class ConfirmationDialogInfo(val title: Int, val message: Int?, val positiveButton: Int, val negativeButton: Int?) {

    constructor(title: Int, message: Int) : this(title, message, R.string.common_enable, R.string.common_cancel)

    companion object {

        fun titleAndButton(title: Int, button: Int) = ConfirmationDialogInfo(title, null, button, null)
    }
}

fun BaseFragmentMixin<*>.setupConfirmationDialog(
    @StyleRes style: Int,
    awaitableMixin: ConfirmationAwaitable<ConfirmationDialogInfo>
) {
    awaitableMixin.awaitableActionLiveData.observeEvent { action ->
        dialog(providedContext, style) {
            setTitle(action.payload.title)
            action.payload.message?.let { setMessage(action.payload.message) }
            setPositiveButton(action.payload.positiveButton) { _, _ -> action.onSuccess(Unit) }

            if (action.payload.negativeButton != null) {
                setNegativeButton(action.payload.negativeButton) { _, _ -> action.onCancel() }
            }

            setOnCancelListener { action.onCancel() }
        }
    }
}

fun BaseFragment<*>.setupConfirmationOrDenyDialog(@StyleRes style: Int, awaitableMixin: ConfirmOrDenyAwaitable<ConfirmationDialogInfo>) {
    awaitableMixin.awaitableActionLiveData.observeEvent { action ->
        dialog(requireContext(), style) {
            setTitle(action.payload.title)
            action.payload.message?.let { setMessage(action.payload.message) }
            setPositiveButton(action.payload.positiveButton) { _, _ -> action.onSuccess(true) }

            if (action.payload.negativeButton != null) {
                setNegativeButton(action.payload.negativeButton) { _, _ -> action.onSuccess(false) }
            }

            setOnCancelListener { action.onCancel() }
        }
    }
}
