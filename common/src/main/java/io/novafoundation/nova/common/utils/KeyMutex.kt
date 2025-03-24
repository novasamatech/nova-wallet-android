package io.novafoundation.nova.common.utils

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class KeyMutex {

    private val mutexesByKey = ConcurrentHashMap<Any, Mutex>()

    suspend inline fun <T> withKeyLock(key: Any, crossinline block: suspend () -> T): T {
        val mutex = getMutexForKey(key)
        return mutex.withLock {
            try {
                block()
            } finally {
                removeMutex(key)
            }
        }
    }

    fun getMutexForKey(key: Any): Mutex {
        return mutexesByKey.getOrPut(key) { Mutex() }
    }

    fun removeMutex(key: Any) {
        mutexesByKey.remove(key)
    }
}
