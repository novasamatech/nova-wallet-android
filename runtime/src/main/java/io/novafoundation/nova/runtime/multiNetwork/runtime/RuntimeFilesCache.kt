package io.novafoundation.nova.runtime.multiNetwork.runtime

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.interfaces.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val DEFAULT_FILE_NAME = "default"

private const val METADATA_FILE_MASK = "metadata_%s"
private const val TYPE_DEFINITIONS_FILE_MASK = "definitions_%s"

// Non-migrated versions are stored in non-opaque format
private const val IS_METADATA_OPAQUE_DEFAULT = false

class RuntimeFilesCache(
    private val fileProvider: FileProvider,
    private val preferences: Preferences,
) {

    suspend fun getBaseTypes(): String {
        return readCacheFile(TYPE_DEFINITIONS_FILE_MASK.format(DEFAULT_FILE_NAME))
    }

    suspend fun getChainTypes(chainId: String): String {
        return readCacheFile(TYPE_DEFINITIONS_FILE_MASK.format(chainId))
    }

    suspend fun getChainMetadata(chainId: String): RawRuntimeMetadata {
        return RawRuntimeMetadata(
            metadataContent = readCacheFileBytes(METADATA_FILE_MASK.format(chainId)),
            isOpaque = isMetadataOpaque(chainId)
        )
    }

    suspend fun saveBaseTypes(types: String) {
        writeToCacheFile(TYPE_DEFINITIONS_FILE_MASK.format(DEFAULT_FILE_NAME), types)
    }

    suspend fun saveChainTypes(chainId: String, types: String) {
        val fileName = TYPE_DEFINITIONS_FILE_MASK.format(chainId)

        writeToCacheFile(fileName, types)
    }

    suspend fun saveChainMetadata(chainId: String, metadata: RawRuntimeMetadata) {
        val fileName = METADATA_FILE_MASK.format(chainId)
        writeToCacheFile(fileName, metadata.metadataContent)
        saveMetadataOpaque(chainId, metadata.isOpaque)
    }

    private suspend fun writeToCacheFile(name: String, content: String) {
        return withContext(Dispatchers.IO) {
            getCacheFile(name).writeText(content)
        }
    }

    private suspend fun writeToCacheFile(name: String, content: ByteArray) {
        return withContext(Dispatchers.IO) {
            getCacheFile(name).writeBytes(content)
        }
    }

    private suspend fun readCacheFile(name: String): String {
        return withContext(Dispatchers.IO) {
            getCacheFile(name).readText()
        }
    }

    private suspend fun readCacheFileBytes(name: String): ByteArray {
        return withContext(Dispatchers.IO) {
            getCacheFile(name).readBytes()
        }
    }

    private suspend fun getCacheFile(name: String): File {
        return withContext(Dispatchers.IO) { fileProvider.getFileInInternalCacheStorage(name) }
    }

    private fun isMetadataOpaque(chainId: String): Boolean {
        return preferences.getBoolean(isMetadataOpaqueKey(chainId), IS_METADATA_OPAQUE_DEFAULT)
    }

    private fun saveMetadataOpaque(chainId: String, isOpaque: Boolean) {
        return preferences.putBoolean(isMetadataOpaqueKey(chainId), isOpaque)
    }

    private fun isMetadataOpaqueKey(chainId: String): String {
        return "RuntimeFilesCache.opaqueMetadata.$chainId"
    }
}
