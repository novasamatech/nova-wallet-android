package io.novafoundation.nova.common.utils

class Consumer<T>(private var target: T?) {

    fun get(): T? {
        val returnable = target
        target = null
        return returnable
    }
}
