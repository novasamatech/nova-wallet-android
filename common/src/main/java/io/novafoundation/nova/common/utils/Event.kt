package io.novafoundation.nova.common.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

class Event<T>(private val content: T) {

    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): T = content
}

class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<Event<T>> {
    override fun onChanged(event: Event<T>?) {
        event?.getContentIfNotHandled()?.let { value ->
            onEventUnhandledContent(value)
        }
    }
}

fun <T> T.event(): Event<T> = Event(this)

fun <T, R> LiveData<Event<T>>.mapEvent(mapper: (T) -> R): LiveData<Event<R>> = this.map { Event(mapper(it.peekContent())) }
