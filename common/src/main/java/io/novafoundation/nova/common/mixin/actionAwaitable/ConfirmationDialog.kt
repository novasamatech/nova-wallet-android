package io.novafoundation.nova.common.mixin.actionAwaitable

import android.content.Context
import androidx.annotation.StyleRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.view.dialog.dialog

sealed interface ConfirmationDialogInfo {

    fun asText(context: Context): ByText

    data class ByRes(val title: Int, val message: Int?, val positiveButton: Int, val negativeButton: Int?) : ConfirmationDialogInfo {
        constructor(title: Int, message: Int) : this(title, message, R.string.common_enable, R.string.common_cancel)

        override fun asText(context: Context): ByText = ByText(
            context.getString(title),
            message?.let { context.getString(message) },
            context.getString(positiveButton),
            negativeButton?.let { context.getString(it) }
        )
    }

    data class ByText(val title: String, val message: String?, val positiveButton: String, val negativeButton: String?) : ConfirmationDialogInfo {
        override fun asText(context: Context): ByText = this
    }

    companion object {

        fun titleAndButton(title: Int, button: Int) = ByRes(title, null, button, null)
    }
}

fun BaseFragmentMixin<*>.setupConfirmationDialog(
    @StyleRes style: Int,
    awaitableMixin: ConfirmationAwaitable<ConfirmationDialogInfo>
) {
    awaitableMixin.awaitableActionLiveData.observeEvent { action ->
        dialog(providedContext, style) {
            val payload = action.payload.asText(context)
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
            val payload = action.payload.asText(context)
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
