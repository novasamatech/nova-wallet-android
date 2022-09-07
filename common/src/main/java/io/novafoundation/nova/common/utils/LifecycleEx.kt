package io.novafoundation.nova.common.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

fun Lifecycle.onDestroy(action: () -> Unit) {
    addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            action()

            removeObserver(this)
        }
    })
}

fun Lifecycle.whenStarted(action: () -> Unit) {
    if (currentState.isAtLeast(Lifecycle.State.STARTED)) {
        action()
    } else {
        addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                action()

                removeObserver(this)
            }
        })
    }
}
