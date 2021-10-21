package io.novafoundation.nova.common.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.asLiveData
import io.novafoundation.nova.common.validation.ProgressConsumer
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlin.coroutines.CoroutineContext

typealias TitleAndMessage = Pair<String, String>

open class BaseViewModel : ViewModel(), CoroutineScope {

    private val _errorLiveData = MutableLiveData<Event<String>>()
    val errorLiveData: LiveData<Event<String>> = _errorLiveData

    private val _errorWithTitleLiveData = MutableLiveData<Event<TitleAndMessage>>()
    val errorWithTitleLiveData: LiveData<Event<TitleAndMessage>> = _errorWithTitleLiveData

    private val _messageLiveData = MutableLiveData<Event<String>>()
    val messageLiveData: LiveData<Event<String>> = _messageLiveData

    fun showMessage(text: String) {
        _messageLiveData.value = Event(text)
    }

    fun showError(title: String, text: String) {
        _errorWithTitleLiveData.value = Event(title to text)
    }

    fun showError(text: String) {
        _errorLiveData.value = Event(text)
    }

    fun showError(throwable: Throwable) {
        throwable.message?.let(this::showError)
    }

    override val coroutineContext: CoroutineContext
        get() = viewModelScope.coroutineContext

    fun <T> Flow<T>.asLiveData(): LiveData<T> {
        return asLiveData(viewModelScope)
    }

    fun <T> Flow<T>.share() = shareIn(viewModelScope, started = SharingStarted.Eagerly, replay = 1)

    suspend fun <P, S> ValidationExecutor.requireValid(
        validationSystem: ValidationSystem<P, S>,
        payload: P,
        validationFailureTransformer: (S) -> TitleAndMessage,
        progressConsumer: ProgressConsumer? = null,
        autoFixPayload: (original: P, failureStatus: S) -> P = { original, _ -> original },
        block: (P) -> Unit,
    ) = requireValid(
        validationSystem = validationSystem,
        payload = payload,
        errorDisplayer = ::showError,
        validationFailureTransformer = validationFailureTransformer,
        progressConsumer = progressConsumer,
        autoFixPayload = autoFixPayload,
        block = block
    )
}
