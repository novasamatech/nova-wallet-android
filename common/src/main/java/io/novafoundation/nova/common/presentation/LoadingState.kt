package io.novafoundation.nova.common.presentation

import io.novafoundation.nova.common.utils.withLoading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

sealed class LoadingState<T> {

    class Loading<T> : LoadingState<T>()

    class Loaded<T>(val data: T) : LoadingState<T>()
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

val <T> LoadingState<T>.dataOrNull: T?
    get() = when (this) {
        is LoadingState.Loaded -> this.data
        else -> null
    }
