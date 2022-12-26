package io.novafoundation.nova.common.utils.sequrity

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.novafoundation.nova.common.data.storage.Preferences
import java.util.concurrent.TimeUnit

class BackgroundAccessObserver(
    private val preferences: Preferences,
    private val accessTimeInBackground: Long = DEFAULT_ACCESS_TIME
) : DefaultLifecycleObserver {

    companion object {
        val DEFAULT_ACCESS_TIME = TimeUnit.MINUTES.toMillis(5L)

        private const val PREFS_ON_PAUSE_TIME = "ON_PAUSE_TIME"
    }

    var subscribed = false
    val subscribers: ArrayList<Callback> = arrayListOf()

    fun subscribe(callback: Callback) {
        subscribers.add(callback)
        if (!subscribed) {
            subscribed = true
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        }
    }

    fun unsubscribe(callback: Callback) {
        subscribers.remove(callback)
        if (subscribed && subscribers.isEmpty()) {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        preferences.removeField(PREFS_ON_PAUSE_TIME)
    }

    override fun onStop(owner: LifecycleOwner) {
        val elapsedTime = SystemClock.elapsedRealtime()
        preferences.putLong(PREFS_ON_PAUSE_TIME, elapsedTime)
    }

    override fun onStart(owner: LifecycleOwner) {
        val elapsedTime = SystemClock.elapsedRealtime()
        val onPauseTime = preferences.getLong(PREFS_ON_PAUSE_TIME, -1)
        val difference = elapsedTime - onPauseTime
        if (onPauseTime >= 0 && difference > accessTimeInBackground) {
            notifyEveryone()
        }
    }

    private fun notifyEveryone() {
        subscribers.forEach { it.onRequestAccess() }
    }

    interface Callback {
        fun onRequestAccess()
    }
}
