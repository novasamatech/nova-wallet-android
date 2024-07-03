package io.novafoundation.nova.common.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun interface ResultableSingleton<T> {

    suspend fun get(): T
}

class ResultableSingletonAction<T>(private val action: suspend () -> T) : ResultableSingleton<T> {

    private var result: T? = null
    private val mutex = Mutex()

    override suspend fun get(): T {
        return mutex.withLock {
            if (result == null) {
                result = action()
                result!!
            } else {
                result!!
            }
        }
    }
}

fun <T> singletonAction(action: suspend () -> T): ResultableSingleton<T> {
    return ResultableSingletonAction(action)
}
