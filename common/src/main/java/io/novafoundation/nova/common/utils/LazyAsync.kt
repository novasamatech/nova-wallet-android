package io.novafoundation.nova.common.utils

fun interface LazyAsync<T> {

    suspend fun get(): T
}

fun <T> lazyAsync(
    mode: AsyncLazyThreadSafetyMode = AsyncLazyThreadSafetyMode.NONE,
    initializer: suspend () -> T
): LazyAsync<T> {
    return when (mode) {
        AsyncLazyThreadSafetyMode.NONE -> UnsafeLazyAsync(initializer)
    }
}


/**
 * @see [LazyThreadSafetyMode]
 */
enum class AsyncLazyThreadSafetyMode {

    NONE,
}

private object UNINITIALIZED_VALUE

private class UnsafeLazyAsync<T>(
    initializer: suspend () -> T
) : LazyAsync<T> {

    private var initializer: (suspend () -> T)? = initializer
    private var value: Any? = UNINITIALIZED_VALUE

    override suspend fun get(): T {
        if (value === UNINITIALIZED_VALUE) {
            value = initializer!!()
            initializer = null
        }

        @Suppress("UNCHECKED_CAST")
        return value as T
    }
}
