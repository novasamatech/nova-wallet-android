package io.novafoundation.nova.common.presentation

import io.novafoundation.nova.common.utils.withLoading
import kotlinx.coroutines.flow.Flow
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
