package io.novafoundation.nova.common.data.memory

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface SingleValueCache<T> {

    suspend operator fun invoke(): T
}

typealias SingleValueCacheCompute<T> = suspend () -> T

fun <T> SingleValueCache(compute: SingleValueCacheCompute<T>): SingleValueCache<T> {
    return RealSingleValueCache(compute)
}

private class RealSingleValueCache<T>(
    private val compute: SingleValueCacheCompute<T>,
) : SingleValueCache<T> {

    private val mutex = Mutex()
    private var cache: Any? = NULL

    @Suppress("UNCHECKED_CAST")
    override suspend operator fun invoke(): T {
        mutex.withLock {
            if (cache === NULL) {
                cache = compute()
            }

            return cache as T
        }
    }

    private object NULL
}

