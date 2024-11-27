package io.novafoundation.nova.common.utils

import android.util.LruCache

class CallbackLruCache<K, V>(maxSize: Int) : LruCache<K, V>(maxSize) {

    private var callback: ((V) -> Unit)? = null

    fun setCallback(callback: (V) -> Unit) {
        this.callback = callback
    }

    override fun entryRemoved(evicted: Boolean, key: K, oldValue: V, newValue: V) {
        callback?.invoke(oldValue)
    }
}
