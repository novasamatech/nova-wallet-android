package io.novafoundation.nova.common.mixin.api

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.utils.Event

interface Browserable {
    val openBrowserEvent: LiveData<Event<String>>

    interface Presentation {
        fun showBrowser(url: String)
    }
}
