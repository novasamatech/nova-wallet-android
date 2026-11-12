package io.novafoundation.nova.feature_swap_impl.presentation.common.state

import android.util.Log
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapOperationPayload
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

@ScreenScope
class SwapOperationLoader @Inject constructor(
    private val payload: SwapOperationPayload,
    private val mapper: SwapOperationPayloadMapper,
    private val resourceManager: ResourceManager,
    private val router: SwapRouter,
) : Retriable {

    override val retryEvent = MutableLiveData<Event<RetryPayload>>()

    val loading = MutableStateFlow(true)

    fun state(scope: CoroutineScope) = flowOf { mapper.fromPayload(payload, scope) }
        .onStart { loading.value = true }
        .onEach { loading.value = false }
        .retryWhen { error, _ ->
            if (error is CancellationException) throw error
            Log.e("Swaps", "Could not restore the reviewed operation", error)
            loading.value = false
            suspendCancellableCoroutine { continuation ->
                retryEvent.postValue(
                    Event(
                        RetryPayload(
                            title = resourceManager.getString(R.string.common_error_general_title),
                            message = resourceManager.getString(R.string.common_undefined_error_message),
                            onRetry = { if (continuation.isActive) continuation.resume(Unit) },
                            onCancel = {
                                router.back()
                                continuation.cancel()
                            }
                        )
                    )
                )
            }
            true
        }
        .onCompletion { loading.value = false }
}
