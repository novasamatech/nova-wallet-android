package io.novafoundation.nova.common.interfaces

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface FileCache {

    suspend fun updateCache(fileName: String, value: String)

    fun observeCachedValue(fileName: String): Flow<String>

    suspend fun getCachedValue(fileName: String): String?
}

private typealias OnCacheValueChanged = (String) -> Unit

internal class InternalFileSystemCache(
    private val fileProvider: FileProvider
) : FileCache {

    private val callbacks: MutableMap<String, MutableList<OnCacheValueChanged>> = mutableMapOf()
    private val fileMutex: Mutex = Mutex()

    override suspend fun updateCache(fileName: String, value: String) {
        fileProvider.writeCache(fileName, value)

        notifyCallbacks(fileName, value)
    }

    override fun observeCachedValue(fileName: String): Flow<String> {
        return callbackFlow<String?> {
            val callback: OnCacheValueChanged = {
                trySend(it)
            }

            putCallback(fileName, callback)

            awaitClose { removeCallback(fileName, callback) }
        }
            .onStart { emit(fileProvider.readCache(fileName)) }
            .filterNotNull()
    }

    override suspend fun getCachedValue(fileName: String): String? {
       return fileProvider.readCache(fileName)
    }

    private fun putCallback(fileName: String, callback: OnCacheValueChanged) = synchronized(this) {
        val callbacksForFile = callbacks.getOrPut(fileName) { mutableListOf() }

        callbacksForFile.add(callback)
    }

    private fun removeCallback(fileName: String, callback: OnCacheValueChanged) = synchronized(this) {
        val callbacksForFile = callbacks[fileName] ?: return

        callbacksForFile.remove(callback)

        if (callbacksForFile.isEmpty()) {
            callbacks.remove(fileName)
        }
    }

    private fun notifyCallbacks(fileName: String, value: String) {
        val callbacks = synchronized(this) { callbacks[fileName]?.toMutableList() }

        callbacks?.forEach { it.invoke(value) }
    }

    private suspend fun FileProvider.readCache(fileName: String): String? = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val file = getFileInInternalCacheStorage(fileName)

            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        }
    }

    private suspend fun FileProvider.writeCache(fileName: String, value: String) = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val file = getFileInInternalCacheStorage(fileName)

            file.writeText(value)
        }
    }
}
