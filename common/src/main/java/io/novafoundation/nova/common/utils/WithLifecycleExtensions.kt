package io.novafoundation.nova.common.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData

interface WithLifecycleExtensions {

    val lifecycleOwner: LifecycleOwner

    fun <V> LiveData<Event<V>>.observeEvent(observer: (V) -> Unit) {
        observe(lifecycleOwner, EventObserver(observer::invoke))
    }
}
