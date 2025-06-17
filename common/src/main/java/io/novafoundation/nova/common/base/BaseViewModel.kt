package io.novafoundation.nova.common.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.errors.shouldIgnore
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.validation.ProgressConsumer
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystem
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

typealias TitleAndMessage = Pair<String, CharSequence?>

open class BaseViewModel :
    ViewModel(),
    CoroutineScope,
    ComputationalScope,
    WithCoroutineScopeExtensions {

    private val _errorLiveData = MutableLiveData<Event<String>>()
    val errorLiveData: LiveData<Event<String>> = _errorLiveData

    private val _errorWithTitleLiveData = MutableLiveData<Event<TitleAndMessage>>()
    val errorWithTitleLiveData: LiveData<Event<TitleAndMessage>> = _errorWithTitleLiveData

    private val _messageLiveData = MutableLiveData<Event<String>>()
    val messageLiveData: LiveData<Event<String>> = _messageLiveData

    private val _toastLiveData = MutableLiveData<Event<String>>()
    val toastLiveData: LiveData<Event<String>> = _toastLiveData

    fun showRetryDialog(text: String) {
        _messageLiveData.postValue(Event(text))
    }

    fun showToast(text: String) {
        _toastLiveData.postValue(Event(text))
    }

    fun showMessage(text: String) {
        _messageLiveData.postValue(Event(text))
    }

    fun showError(title: String, text: CharSequence) {
        _errorWithTitleLiveData.postValue(Event(title to text))
    }

    fun showError(text: String) {
        _errorLiveData.postValue(Event(text))
    }

    fun showError(throwable: Throwable) {
        if (!shouldIgnore(throwable)) {
            throwable.printStackTrace()

            throwable.message?.let(this::showError)
        }
    }

    override val coroutineContext: CoroutineContext
        get() = viewModelScope.coroutineContext

    override val coroutineScope: CoroutineScope
        get() = this

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
        errorDisplayer = { showError(it) },
        validationFailureTransformerDefault = validationFailureTransformer,
        progressConsumer = progressConsumer,
        autoFixPayload = autoFixPayload,
        block = block,
        scope = viewModelScope
    )

    suspend fun <P, S> ValidationExecutor.requireValid(
        validationSystem: ValidationSystem<P, S>,
        payload: P,
        validationFailureTransformerCustom: (ValidationStatus.NotValid<S>, ValidationFlowActions<P>) -> TransformedFailure?,
        autoFixPayload: (original: P, failureStatus: S) -> P = { original, _ -> original },
        progressConsumer: ProgressConsumer? = null,
        block: (P) -> Unit,
    ) = requireValid(
        validationSystem = validationSystem,
        payload = payload,
        errorDisplayer = ::showError,
        validationFailureTransformerCustom = validationFailureTransformerCustom,
        progressConsumer = progressConsumer,
        autoFixPayload = autoFixPayload,
        block = block,
        scope = viewModelScope
    )
}
