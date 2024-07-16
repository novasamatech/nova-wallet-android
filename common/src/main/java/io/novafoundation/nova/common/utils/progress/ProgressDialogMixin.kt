package io.novafoundation.nova.common.utils.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event

class ProgressDialogMixinFactory {

    fun create(): ProgressDialogMixin = RealProgressDialogMixin()
}

interface ProgressDialogMixin {

    val showProgressLiveData: LiveData<Event<ProgressState>>

    fun setProgressState(state: ProgressState)
}

class RealProgressDialogMixin : ProgressDialogMixin {

    private val _showProgressLiveData = MutableLiveData<Event<ProgressState>>()

    override val showProgressLiveData: LiveData<Event<ProgressState>> = _showProgressLiveData

    override fun setProgressState(state: ProgressState) {
        _showProgressLiveData.value = state.event()
    }
}

sealed interface ProgressState {

    class Show(val textRes: Int) : ProgressState

    object Hide : ProgressState
}

suspend fun ProgressDialogMixin.startProgress(progressTextRes: Int, action: suspend () -> Unit) {
    setProgressState(ProgressState.Show(progressTextRes))
    action()
    setProgressState(ProgressState.Hide)
}
