package io.novafoundation.nova.common.mixin.copy

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.mixin.copy.CopyTextLauncher.Payload
import io.novafoundation.nova.common.utils.Event

interface CopyTextLauncher {

    class Payload(val title: String, val textToCopy: String, val copyButtonName: String, val shareButtonName: String)

    val showCopyTextDialog: LiveData<Event<Payload>>

    interface Presentation : CopyTextLauncher {

        suspend fun showCopyTextDialog(payload: Payload)
    }
}

class RealCopyTextLauncher : CopyTextLauncher.Presentation {

    override val showCopyTextDialog = MutableLiveData<Event<Payload>>()

    override suspend fun showCopyTextDialog(payload: Payload) {
        showCopyTextDialog.value = Event(payload)
    }
}
