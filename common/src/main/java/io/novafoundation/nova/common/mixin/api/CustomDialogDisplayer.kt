package io.novafoundation.nova.common.mixin.api

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.utils.Event

interface CustomDialogDisplayer {

    val showCustomDialog: LiveData<Event<Payload>>

    class Payload(
        val title: String,
        val message: String,
        val okAction: DialogAction,
        val cancelAction: DialogAction? = null,
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
