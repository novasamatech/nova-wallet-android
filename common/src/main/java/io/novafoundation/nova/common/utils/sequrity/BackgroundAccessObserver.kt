package io.novafoundation.nova.common.utils.sequrity

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.novafoundation.nova.common.data.storage.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class BackgroundAccessObserver(
    private val preferences: Preferences,
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val accessTimeInBackground: Long = DEFAULT_ACCESS_TIME,
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

    private var currentState = State.NOTHING

    private val _stateFlow = MutableSharedFlow<State>()

    val requestAccessFlow: Flow<State> = _stateFlow
        .filter { it == State.REQUEST_ACCESS }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun checkPassed() {
        changeState(State.NOTHING)
        preferences.removeField(PREFS_ON_PAUSE_TIME)
        automaticInteractionGate.foregroundCheckPassed()
    }

    override fun onCreate(owner: LifecycleOwner) {
        preferences.removeField(PREFS_ON_PAUSE_TIME)
    }

    override fun onStop(owner: LifecycleOwner) {
        val elapsedTime = SystemClock.elapsedRealtime()
        preferences.putLong(PREFS_ON_PAUSE_TIME, elapsedTime)
        automaticInteractionGate.wentToBackground()
    }

    override fun onStart(owner: LifecycleOwner) {
        val elapsedTime = SystemClock.elapsedRealtime()
        val onPauseTime = preferences.getLong(PREFS_ON_PAUSE_TIME, -1)
        val difference = elapsedTime - onPauseTime
        if (onPauseTime >= 0 && difference > accessTimeInBackground) {
            changeState(State.REQUEST_ACCESS)
        } else {
            automaticInteractionGate.foregroundCheckPassed()
        }
    }

    private fun changeState(state: State) {
        launch {
            currentState = state
            _stateFlow.emit(currentState)
        }
    }
}
