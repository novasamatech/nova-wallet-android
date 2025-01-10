package io.novafoundation.nova.common.utils

class Consumer<T>(private var target: T?) {

    fun getOnce(): T? {
        return target.also {
            target = null
        }
    }

    fun useOnce(block: (T) -> Unit) {
        getOnce()?.let(block)
    }
}
