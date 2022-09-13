package io.novafoundation.nova.common.presentation

import io.novafoundation.nova.common.utils.accumulate
import io.novafoundation.nova.common.utils.withLoading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

sealed class LoadingState<out T> {

    class Loading<T> : LoadingState<T>()

    data class Loaded<T>(val data: T) : LoadingState<T>()
}

interface LoadingView<T> {

    fun showData(data: T)

    fun showLoading()
}

fun <T> LoadingView<T>.showLoadingState(loadingState: LoadingState<T>) {
    when (loadingState) {
        is LoadingState.Loaded -> showData(loadingState.data)
        is LoadingState.Loading -> showLoading()
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <T, R> LoadingState<T>.map(mapper: (T) -> R): LoadingState<R> {
    return when (this) {
        is LoadingState.Loading<*> -> this as LoadingState.Loading<R>
        is LoadingState.Loaded<T> -> LoadingState.Loaded(mapper(data))
    }
}

@Suppress("UNCHECKED_CAST")
fun <T, V> Flow<LoadingState<T>>.flatMapLoading(mapper: (T) -> Flow<V>): Flow<LoadingState<V>> {
    return flatMapLatest {
        when (it) {
            is LoadingState.Loading<*> -> flowOf(it as LoadingState.Loading<V>)
            is LoadingState.Loaded<T> -> mapper(it.data).withLoading()
        }
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <T, V> Flow<LoadingState<T>>.mapLoading(crossinline mapper: suspend (T) -> V): Flow<LoadingState<V>> {
    return map { loadingState -> loadingState.map { mapper(it) } }
}

fun <T> T?.toLoadingState(): LoadingState<T> = if (this == null) LoadingState.Loading() else LoadingState.Loaded(this)

@Suppress("UNCHECKED_CAST")
fun <T1, T2, R> combineLoading(
    source1: Flow<LoadingState<T1>>,
    source2: Flow<LoadingState<T2>>,
    combiner: suspend (T1, T2) -> R,
): Flow<LoadingState<R>> = combine(source1, source2) { state1, state2 ->
    if (state1 is LoadingState.Loaded && state2 is LoadingState.Loaded) {
        LoadingState.Loaded(combiner(state1.data, state2.data))
    } else {
        LoadingState.Loading()
    }
}

fun <T> firstNonEmptyLoading(
    vararg sources: Flow<LoadingState<List<T>>>
): Flow<LoadingState<List<T>>> = accumulate(*sources)
    .map { loadingStates ->
        val isAllLoaded = loadingStates.all { it is LoadingState.Loaded }
        val states: List<List<T>> = loadingStates.mapNotNull {
            if (it is LoadingState.Loaded && it.data.isNotEmpty()) {
                it.data
            } else {
                null
            }
        }

        if (isAllLoaded || states.isNotEmpty()) {
            LoadingState.Loaded(states.flatten())
        } else {
            LoadingState.Loading()
        }
    }

val <T> LoadingState<T>.dataOrNull: T?
    get() = when (this) {
        is LoadingState.Loaded -> this.data
        else -> null
    }
