package io.novafoundation.nova.common.mixin.copy

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event

interface CopyTextMixin {

    val showCopyTextDialog: LiveData<Event<String>>

    interface Presentation : CopyTextMixin {

        suspend fun showCopyTextDialog(text: String)
    }
}

class CopyTextMixinProvider : CopyTextMixin.Presentation {

    override val showCopyTextDialog = MutableLiveData<Event<String>>()

    override suspend fun showCopyTextDialog(text: String) {
        showCopyTextDialog.value = Event(text)
    }
}
