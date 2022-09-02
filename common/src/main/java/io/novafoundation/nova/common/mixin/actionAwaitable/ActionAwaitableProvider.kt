package io.novafoundation.nova.common.mixin.actionAwaitable

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class ActionAwaitableProvider<P, R> : ActionAwaitableMixin.Presentation<P, R> {

    companion object : ActionAwaitableMixin.Factory {

        override fun <P, R> create(): ActionAwaitableMixin.Presentation<P, R> {
            return ActionAwaitableProvider()
        }
    }

    override val awaitableActionLiveData = MutableLiveData<Event<ActionAwaitableMixin.Action<P, R>>>()

    override suspend fun awaitAction(payload: P): R = suspendCancellableCoroutine { continuation ->
        val action = ActionAwaitableMixin.Action<P, R>(
            payload = payload,
            onSuccess = { continuation.resume(it) },
            onCancel = { continuation.cancel() }
        )

        awaitableActionLiveData.postValue(action.event())
    }
}
