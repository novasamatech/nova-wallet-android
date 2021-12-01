package io.novafoundation.nova.feature_account_api.presenatation.mixin.importType

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event

class ImportTypeChooserProvider : ImportTypeChooserMixin.Presentation {

    override fun showChooser(payload: ImportTypeChooserMixin.Payload) {
        showChooserEvent.value = Event(payload)
    }

    override val showChooserEvent = MutableLiveData<Event<ImportTypeChooserMixin.Payload>>()
}
