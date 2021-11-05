package io.novafoundation.nova.common.utils

import android.widget.CompoundButton
import android.widget.EditText
import android.widget.RadioGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.presentation.LoadingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch

inline fun <T, R> Flow<List<T>>.mapList(crossinline mapper: suspend (T) -> R) = map { list ->
    list.map { item -> mapper(item) }
}

/**
 * Modifies flow so that it firstly emits [LoadingState.Loading] state.
 * Then emits each element from upstream wrapped into [LoadingState.Loaded] state.
 */
fun <T> Flow<T>.withLoading(): Flow<LoadingState<T>> {
    return map<T, LoadingState<T>> { LoadingState.Loaded(it) }
        .onStart { emit(LoadingState.Loading()) }
}

fun <T1, T2> combineToPair(flow1: Flow<T1>, flow2: Flow<T2>): Flow<Pair<T1, T2>> = combine(flow1, flow2, ::Pair)

/**
 * Modifies flow so that it firstly emits [LoadingState.Loading] state for each element from upstream.
 * Then, it constructs new source via [sourceSupplier] and emits all of its items wrapped into [LoadingState.Loaded] state
 * Old suppliers are discarded as per [Flow.transformLatest] behavior
 */
fun <T, R> Flow<T>.withLoading(sourceSupplier: suspend (T) -> Flow<R>): Flow<LoadingState<R>> {
    return transformLatest { item ->
        emit(LoadingState.Loading<R>())

        val newSource = sourceSupplier(item).map { LoadingState.Loaded(it) }

        emitAll(newSource)
    }
}

/**
 * Similar to [Flow.takeWhile] but emits last element too
 */
fun <T> Flow<T>.takeWhileInclusive(predicate: suspend (T) -> Boolean) = transformWhile {
    emit(it)

    predicate(it)
}

/**
 * Modifies flow so that it firstly emits [LoadingState.Loading] state for each element from upstream.
 * Then, it constructs new source via [sourceSupplier] and emits all of its items wrapped into [LoadingState.Loaded] state
 * Old suppliers are discarded as per [Flow.transformLatest] behavior
 */
fun <T, R> Flow<T>.withLoadingSingle(sourceSupplier: suspend (T) -> R): Flow<LoadingState<R>> {
    return transformLatest { item ->
        emit(LoadingState.Loading<R>())

        val newSource = LoadingState.Loaded(sourceSupplier(item))

        emit(newSource)
    }
}

fun <T> Flow<T>.asLiveData(scope: CoroutineScope): LiveData<T> {
    val liveData = MutableLiveData<T>()

    onEach {
        liveData.value = it
    }.launchIn(scope)

    return liveData
}

data class ListDiff<T>(
    val removed: List<T>,
    val addedOrModified: List<T>,
    val all: List<T>
)

fun <T> Flow<List<T>>.diffed(): Flow<ListDiff<T>> {
    return zipWithPrevious().map { (previous, new) ->
        val addedOrModified = new - previous.orEmpty()
        val removed = previous.orEmpty() - new

        ListDiff(removed = removed, addedOrModified = addedOrModified, all = new)
    }
}

fun <T> Flow<T>.zipWithPrevious(): Flow<Pair<T?, T>> = flow {
    var current: T? = null

    collect {
        emit(current to it)

        current = it
    }
}

fun <T> singleReplaySharedFlow() = MutableSharedFlow<T>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

fun <T> Flow<T>.inBackground() = flowOn(Dispatchers.Default)

fun EditText.bindTo(flow: MutableStateFlow<String>, scope: CoroutineScope) {
    scope.launch {
        flow.collect { input ->
            if (text.toString() != input) {
                setText(input)
            }
        }
    }

    onTextChanged {
        scope.launch {
            flow.emit(it)
        }
    }
}

inline fun MutableStateFlow<Boolean>.withFlagSet(action: () -> Unit) {
    value = true

    action()

    value = false
}

fun CompoundButton.bindTo(flow: MutableStateFlow<Boolean>, scope: CoroutineScope) {
    scope.launch {
        flow.collect { newValue ->
            if (isChecked != newValue) {
                isChecked = newValue
            }
        }
    }

    setOnCheckedChangeListener { _, newValue ->
        if (flow.value != newValue) {
            flow.value = newValue
        }
    }
}

fun RadioGroup.bindTo(flow: MutableStateFlow<Int>, scope: LifecycleCoroutineScope) {
    setOnCheckedChangeListener { _, checkedId ->
        if (flow.value != checkedId) {
            flow.value = checkedId
        }
    }

    scope.launchWhenResumed {
        flow.collect {
            if (it != checkedRadioButtonId) {
                check(it)
            }
        }
    }
}

inline fun <T> Flow<T>.observe(
    scope: LifecycleCoroutineScope,
    crossinline collector: suspend (T) -> Unit,
) {
    scope.launchWhenResumed {
        collect(collector)
    }
}

fun MutableStateFlow<Boolean>.toggle() {
    value = !value
}

fun <T> flowOf(producer: suspend () -> T) = flow {
    emit(producer())
}

inline fun <T> Flow<T>.observeInLifecycle(
    lifecycleCoroutineScope: LifecycleCoroutineScope,
    crossinline observer: suspend (T) -> Unit,
) {
    lifecycleCoroutineScope.launchWhenResumed {
        collect(observer)
    }
}

// TODO replace with trySendBlocking from stdlib after upgrade to Kotlin 1.5
// why not offer: https://github.com/Kotlin/kotlinx.coroutines/issues/2550
@ExperimentalCoroutinesApi
fun <E> SendChannel<E>.safeOffer(value: E): Boolean {
    if (isClosedForSend) return false
    return try {
        offer(value)
    } catch (e: CancellationException) {
        false
    }
}
