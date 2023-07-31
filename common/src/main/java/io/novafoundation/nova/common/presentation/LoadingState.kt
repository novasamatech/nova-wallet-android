package io.novafoundation.nova.common.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Deprecated("Use ExtendedLoadingState instead since it offers support for errors too")
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
inline fun <T, R> LoadingState<T>.flatMap(mapper: (T) -> LoadingState<R>): LoadingState<R> {
    return when (this) {
        is LoadingState.Loading<*> -> this as LoadingState.Loading<R>
        is LoadingState.Loaded<T> -> mapper(data)
    }
}


@Suppress("UNCHECKED_CAST")
inline fun <T, V> Flow<LoadingState<T>>.mapLoading(crossinline mapper: suspend (T) -> V): Flow<LoadingState<V>> {
    return map { loadingState -> loadingState.map { mapper(it) } }
}

fun <T> T?.toLoadingState(): LoadingState<T> = if (this == null) LoadingState.Loading() else LoadingState.Loaded(this)

val <T> LoadingState<T>.dataOrNull: T?
    get() = when (this) {
        is LoadingState.Loaded -> this.data
        else -> null
    }

suspend inline fun <reified T> Flow<LoadingState<T>>.firstLoaded(): T = first { it.dataOrNull != null }.dataOrNull as T
