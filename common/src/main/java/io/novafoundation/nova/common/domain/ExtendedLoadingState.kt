package io.novafoundation.nova.common.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.map

sealed class ExtendedLoadingState<out T> {

    companion object;

    object Loading : ExtendedLoadingState<Nothing>()

    data class Error(val exception: Throwable) : ExtendedLoadingState<Nothing>()

    data class Loaded<T>(val data: T) : ExtendedLoadingState<T>()
}

inline fun <T, V> Flow<ExtendedLoadingState<T>>.mapLoading(crossinline mapper: suspend (T) -> V): Flow<ExtendedLoadingState<V>> {
    return map { loadingState -> loadingState.map { mapper(it) } }
}

inline fun <T, R> ExtendedLoadingState<T>.map(mapper: (T) -> R): ExtendedLoadingState<R> {
    return when (this) {
        is ExtendedLoadingState.Loading -> this
        is ExtendedLoadingState.Error -> this
        is ExtendedLoadingState.Loaded<T> -> ExtendedLoadingState.Loaded(mapper(data))
    }
}

val <T> ExtendedLoadingState<T>.dataOrNull: T?
    get() = when (this) {
        is ExtendedLoadingState.Loaded -> this.data
        else -> null
    }

fun <T> loadedNothing(): ExtendedLoadingState<T?> {
    return ExtendedLoadingState.Loaded(null)
}

fun ExtendedLoadingState<*>.isLoading(): Boolean {
    return this is ExtendedLoadingState.Loading
}

fun ExtendedLoadingState<*>.isLoaded(): Boolean {
    return this is ExtendedLoadingState.Loaded
}

suspend fun <T> FlowCollector<ExtendedLoadingState<T>>.emitLoaded(value: T) {
    emit(ExtendedLoadingState.Loaded(value))
}

suspend fun <T> FlowCollector<ExtendedLoadingState<T>>.emitLoading() {
    emit(ExtendedLoadingState.Loading)
}

suspend fun <T> FlowCollector<ExtendedLoadingState<T>>.emitError(throwable: Throwable) {
    emit(ExtendedLoadingState.Error(throwable))
}

fun <T> ExtendedLoadingState.Companion.fromOption(value: T?): ExtendedLoadingState<T> {
    return if (value != null) {
        ExtendedLoadingState.Loaded(value)
    } else {
        ExtendedLoadingState.Loading
    }
}

fun <T> T.asLoaded(): ExtendedLoadingState.Loaded<T> = ExtendedLoadingState.Loaded(this)

inline fun <T> ExtendedLoadingState<T>.onLoaded(action: (T) -> Unit): ExtendedLoadingState<T> {
    if (this is ExtendedLoadingState.Loaded) {
        action(data)
    }

    return this
}

inline fun <T> ExtendedLoadingState<T>.onNotLoaded(action: () -> Unit): ExtendedLoadingState<T> {
    if (this !is ExtendedLoadingState.Loaded) {
        action()
    }

    return this
}

inline fun <T> ExtendedLoadingState<T>.onError(action: (Throwable) -> Unit): ExtendedLoadingState<T> {
    if (this is ExtendedLoadingState.Error) {
        action(exception)
    }

    return this
}
