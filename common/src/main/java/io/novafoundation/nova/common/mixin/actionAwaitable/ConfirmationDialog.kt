package io.novafoundation.nova.common.mixin.actionAwaitable

import androidx.annotation.StyleRes
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.dialog.dialog

class ConfirmationDialogInfo(val title: String, val message: String?, val positiveButton: String, val negativeButton: String?) {

    companion object;
}

fun ConfirmationDialogInfo.Companion.fromRes(
    resourceManager: ResourceManager,
    title: Int,
    message: Int?,
    positiveButton: Int,
    negativeButton: Int?
) = ConfirmationDialogInfo(
    resourceManager.getString(title),
    message?.let { resourceManager.getString(message) },
    resourceManager.getString(positiveButton),
    negativeButton?.let { resourceManager.getString(it) }
)

fun ConfirmationDialogInfo.Companion.titleAndButton(
    resourceManager: ResourceManager,
    title: Int,
    button: Int
) = fromRes(resourceManager, title, null, button, null)

fun BaseFragmentMixin<*>.setupConfirmationDialog(
    @StyleRes style: Int,
    awaitableMixin: ConfirmationAwaitable<ConfirmationDialogInfo>
) {
    awaitableMixin.awaitableActionLiveData.observeEvent { action ->
        dialog(providedContext, style) {
            val payload = action.payload
            setTitle(payload.title)
            payload.message?.let { setMessage(payload.message) }
            setPositiveButton(payload.positiveButton) { _, _ -> action.onSuccess(Unit) }

            if (payload.negativeButton != null) {
                setNegativeButton(payload.negativeButton) { _, _ -> action.onCancel() }
            }

            setOnCancelListener { action.onCancel() }
        }
    }
}

fun BaseFragment<*, *>.setupConfirmationOrDenyDialog(@StyleRes style: Int, awaitableMixin: ConfirmOrDenyAwaitable<ConfirmationDialogInfo>) {
    awaitableMixin.awaitableActionLiveData.observeEvent { action ->
        dialog(requireContext(), style) {
            val payload = action.payload
            setTitle(payload.title)
            payload.message?.let { setMessage(payload.message) }
            setPositiveButton(payload.positiveButton) { _, _ -> action.onSuccess(true) }

            if (payload.negativeButton != null) {
                setNegativeButton(payload.negativeButton) { _, _ -> action.onSuccess(false) }
            }

            setOnCancelListener { action.onCancel() }
        }
    }
}
