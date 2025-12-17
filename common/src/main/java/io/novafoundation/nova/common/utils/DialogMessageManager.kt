package io.novafoundation.nova.common.utils

import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

typealias DialogBuilder = AlertDialog.Builder.() -> Unit

interface DialogMessageManager {

    val dialogMessagesEvents: LiveData<Event<DialogBuilder>>

    fun showDialog(
        @StyleRes customStyle: Int? = null,
        decorator: DialogBuilder
    )
}

/**
 * TODO: Unite this approach with [io.novafoundation.nova.common.view.dialog.dialog]. Create a common interface for example
 */
class RealDialogMessageManager : DialogMessageManager {

    override val dialogMessagesEvents = MutableLiveData<Event<DialogBuilder>>()

    override fun showDialog(
        @StyleRes customStyle: Int?,
        decorator: DialogBuilder
    ) {
        dialogMessagesEvents.value = Event(decorator)
    }
}
