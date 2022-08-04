package io.novafoundation.nova.common.utils

interface SharedState<T> {

    fun getOrNull(): T?
}

fun <T> SharedState<T>.getOrThrow(): T = getOrNull() ?: throw IllegalStateException("State is null")

interface MutableSharedState<T> : SharedState<T> {

    fun set(value: T)

    fun reset()
}

class DefaultMutableSharedState<T> : MutableSharedState<T> {

    @Volatile
    private var value: T? = null

    @Synchronized
    override fun getOrNull(): T? {
        return value
    }

    @Synchronized
    override fun set(value: T) {
        this.value = value
    }

    @Synchronized
    override fun reset() {
        value = null
    }
}
