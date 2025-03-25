package io.novafoundation.nova.common.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

interface ToastMessageManager {

    val toastMessagesEvents: LiveData<Event<String>>

    fun showToast(message: String)
}

class RealToastMessageManager : ToastMessageManager {

    override val toastMessagesEvents = MutableLiveData<Event<String>>()

    override fun showToast(message: String) {
        toastMessagesEvents.value = Event(message)
    }
}
