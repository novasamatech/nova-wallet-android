package io.novafoundation.nova.common.mixin.actionAwaitable

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet

typealias ChooseOneOfManyAwaitable<E> = ActionAwaitableMixin<DynamicListBottomSheet.Payload<E>, E>

interface ActionAwaitableMixin<P, R> {

    class Action<P, R>(
        val payload: P,
        val onSuccess: (R) -> Unit,
        val onCancel: () -> Unit,
    )

    val awaitableActionLiveData: LiveData<Event<Action<P, R>>>

    interface Presentation<P, R> : ActionAwaitableMixin<P, R> {

        suspend fun awaitAction(payload: P): R
    }

    interface Factory {

        fun <P, R> create(): Presentation<P, R>
    }
}

fun <T> ActionAwaitableMixin.Factory.selectingOneOf() = create<DynamicListBottomSheet.Payload<T>, T>()

suspend fun <R> ActionAwaitableMixin.Presentation<Unit, R>.awaitAction() = awaitAction(Unit)
