package io.novafoundation.nova.common.mixin.api

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event

interface Browserable {
    val openBrowserEvent: LiveData<Event<String>>

    interface Presentation: Browserable {
        companion object // extensions

        fun showBrowser(url: String)
    }
}

fun Browserable.Presentation.Companion.of(liveData: MutableLiveData<Event<String>>) = object : Browserable.Presentation {

    override fun showBrowser(url: String) {
        liveData.value = Event(url)
    }

    override val openBrowserEvent = liveData
}

fun Browserable(): Browserable.Presentation = Browserable.Presentation.of(MutableLiveData())
