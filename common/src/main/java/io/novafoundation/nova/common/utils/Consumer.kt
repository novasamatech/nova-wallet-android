package io.novafoundation.nova.common.utils

class Consumer<T>(private var target: T?) {

    fun get(): T? {
        val returnable = target
        target = null
        return returnable
    }

    fun use(block: (T) -> Unit) {
        val target = get()
        if (target != null) {
            block(target)
        }
    }
}
