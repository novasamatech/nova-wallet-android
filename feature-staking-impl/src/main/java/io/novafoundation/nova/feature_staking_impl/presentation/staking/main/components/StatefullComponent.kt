package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface StatefullComponent<STATE, EVENT, ACTION> {

    /**
     * one-shot events
     */
    val events: LiveData<Event<EVENT>>

    /**
     * state will be null if component is not available in the current context
     */
    val state: Flow<STATE?>

    fun onAction(action: ACTION)
}

class UnsupportedComponent<S, E, A> : StatefullComponent<S, E, A> {

    companion object {

        fun <S, E, A> creator(): ComponentCreator<S, E, A> = { _, _ -> UnsupportedComponent() }
    }

    override val events = MutableLiveData<Event<E>>()

    override val state: Flow<S?> = flowOf(null)

    override fun onAction(action: A) {
        // pass
    }
}

interface AwaitableEvent<P, R> {

    val value: ActionAwaitableMixin.Action<P, R>
}

typealias ChooseOneOfAwaitableEvent<E> = AwaitableEvent<List<E>, E>
typealias ChooseOneOfManyAwaitableEvent<E> = AwaitableEvent<DynamicListBottomSheet.Payload<E>, E>

suspend fun <A : AwaitableEvent<P, R>, P, R> MutableLiveData<out Event<in A>>.awaitAction(
    payload: P,
    eventCreator: (ActionAwaitableMixin.Action<P, R>) -> A
): R {
    return suspendCancellableCoroutine { continuation ->
        val action = ActionAwaitableMixin.Action<P, R>(
            payload = payload,
            onSuccess = { continuation.resume(it) },
            onCancel = { continuation.cancel() }
        )

        value = eventCreator(action).event()
    }
}
