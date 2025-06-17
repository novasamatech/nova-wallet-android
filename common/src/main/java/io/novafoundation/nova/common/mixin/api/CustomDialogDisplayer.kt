package io.novafoundation.nova.common.mixin.api

import androidx.annotation.StyleRes
import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer.Payload.DialogAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event

interface CustomDialogDisplayer {

    val showCustomDialog: LiveData<Event<Payload>>

    class Payload(
        val title: String,
        val message: CharSequence?,
        val okAction: DialogAction?,
        val cancelAction: DialogAction? = null,
        @StyleRes val customStyle: Int? = null,
    ) {

        class DialogAction(
            val title: String,
            val action: () -> Unit,
        ) {

            companion object {

                fun noOp(title: String) = DialogAction(title = title, action = {})
            }
        }
    }

    interface Presentation : CustomDialogDisplayer {

        fun displayDialog(payload: Payload)
    }
}

fun CustomDialogDisplayer.Presentation.displayError(
    resourceManager: ResourceManager,
    error: Throwable,
) {
    error.message?.let {
        displayDialog(
            CustomDialogDisplayer.Payload(
                title = resourceManager.getString(R.string.common_error_general_title),
                message = it,
                okAction = DialogAction.noOp(resourceManager.getString(R.string.common_ok)),
                cancelAction = null
            ),
        )
    }
}

fun TitleAndMessage.toCustomDialogPayload(resourceManager: ResourceManager): CustomDialogDisplayer.Payload {
    return CustomDialogDisplayer.Payload(
        title = first,
        message = second,
        okAction = DialogAction.noOp(resourceManager.getString(R.string.common_ok)),
    )
}

fun CustomDialogDisplayer.Presentation.displayDialogOrNothing(payload: CustomDialogDisplayer.Payload?) {
    payload?.let {
        displayDialog(it)
    }
}
