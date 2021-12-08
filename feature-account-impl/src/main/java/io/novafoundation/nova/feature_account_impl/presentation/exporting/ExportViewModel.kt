package io.novafoundation.nova.feature_account_impl.presentation.exporting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.Event

abstract class ExportViewModel : BaseViewModel() {

    private val _exportEvent = MutableLiveData<Event<String>>()
    val exportEvent: LiveData<Event<String>> = _exportEvent

    protected fun exportText(text: String) {
        _exportEvent.value = Event(text)
    }
}
