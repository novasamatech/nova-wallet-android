package io.novafoundation.nova.common.data

import android.content.Context
import android.net.Uri
import io.novafoundation.nova.common.interfaces.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import androidx.core.content.FileProvider as AndroidFileProvider


class FileProviderImpl(
    private val context: Context
) : FileProvider {

    override suspend fun getFileInExternalCacheStorage(fileName: String): File {
        return withContext(Dispatchers.IO) {
            val cacheDir = context.externalCacheDir?.absolutePath ?: directoryNotAvailable()

            File(cacheDir, fileName)
        }
    }

    override suspend fun getFileInInternalCacheStorage(fileName: String): File {
        return withContext(Dispatchers.IO) {
            val cacheDir = context.cacheDir?.absolutePath ?: directoryNotAvailable()

            File(cacheDir, fileName)
        }
    }

    override suspend fun generateTempFile(fixedName: String?): File {
        val name = fixedName ?: UUID.randomUUID().toString()

        return getFileInExternalCacheStorage(name)
    }

    override suspend fun uriOf(file: File): Uri {
        return withContext(Dispatchers.IO) {
            AndroidFileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        }
    }

    private fun directoryNotAvailable(): Nothing {
        throw IllegalStateException("Cache directory is unavailable")
    }
}
