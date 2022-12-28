package io.novafoundation.nova.common.utils.sequrity

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.novafoundation.nova.common.data.storage.Preferences
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class BackgroundAccessObserver(
    private val preferences: Preferences,
    private val accessTimeInBackground: Long = DEFAULT_ACCESS_TIME
) : DefaultLifecycleObserver, CoroutineScope {

    companion object {
        val DEFAULT_ACCESS_TIME = TimeUnit.MINUTES.toMillis(5L)

        private const val PREFS_ON_PAUSE_TIME = "ON_PAUSE_TIME"
    }

    enum class State {
        REQUEST_ACCESS, NOTHING
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private val _requestAccessFlow = MutableStateFlow(State.NOTHING)

    val stateFlow: Flow<State> = _requestAccessFlow

    val currentState: State
        get() = _requestAccessFlow.value

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun onAccessed() {
        _requestAccessFlow.value = State.NOTHING
        preferences.removeField(PREFS_ON_PAUSE_TIME)
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
        launch {
            if (_requestAccessFlow.value == State.REQUEST_ACCESS) {
                _requestAccessFlow.emit(State.REQUEST_ACCESS)
            } else {
                _requestAccessFlow.value = State.REQUEST_ACCESS
            }
        }
    }
}
