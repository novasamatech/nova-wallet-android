package io.novafoundation.nova.common.mixin.actionAwaitable

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin.Action
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

typealias ChooseOneOfManyAwaitable<E> = ActionAwaitableMixin<DynamicListBottomSheet.Payload<E>, E>
typealias ConfirmationAwaitable<P> = ActionAwaitableMixin.Presentation<P, Unit>

typealias ConfirmOrDenyAwaitable<P> = ActionAwaitableMixin.Presentation<P, Boolean>

typealias ChooseOneOfAwaitableAction<E> = Action<List<E>, E>
typealias ChooseOneOfManyAwaitableAction<E> = Action<DynamicListBottomSheet.Payload<E>, E>

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

val <P, R> ActionAwaitableMixin<P, R>.awaitableActionFlow: Flow<Action<P, R>>
    get() = awaitableActionLiveData.asFlow()
        .mapNotNull { it.getContentIfNotHandled() }

fun <T> ActionAwaitableMixin.Factory.selectingOneOf() = create<DynamicListBottomSheet.Payload<T>, T>()
fun <P> ActionAwaitableMixin.Factory.confirmingAction(): ConfirmationAwaitable<P> = create()
fun <P> ActionAwaitableMixin.Factory.confirmingOrDenyingAction(): ConfirmOrDenyAwaitable<P> = create()

fun <T> ActionAwaitableMixin.Factory.fixedSelectionOf() = create<Unit, T>()

suspend fun <R> ActionAwaitableMixin.Presentation<Unit, R>.awaitAction() = awaitAction(Unit)

fun ActionAwaitableMixin<*, *>.hasAlreadyTriggered(): Boolean = awaitableActionLiveData.value != null
