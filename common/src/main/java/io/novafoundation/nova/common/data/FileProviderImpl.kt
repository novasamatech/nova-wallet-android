package io.novafoundation.nova.common.data

import android.content.Context
import android.net.Uri
import io.novafoundation.nova.common.interfaces.FileProvider
import java.io.File
import java.util.UUID
import androidx.core.content.FileProvider as AndroidFileProvider

class FileProviderImpl(
    private val context: Context
) : FileProvider {

    override fun getFileInExternalCacheStorage(fileName: String): File {
        val cacheDir = context.externalCacheDir?.absolutePath ?: directoryNotAvailable()

        return File(cacheDir, fileName)
    }

    override fun getFileInInternalCacheStorage(fileName: String): File {
        val cacheDir = context.cacheDir?.absolutePath ?: directoryNotAvailable()

        return File(cacheDir, fileName)
    }

    override fun generateTempFile(fixedName: String?): File {
        val name = fixedName ?: UUID.randomUUID().toString()

        return getFileInExternalCacheStorage(name)
    }

    override fun uriOf(file: File): Uri {
        return AndroidFileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    private fun directoryNotAvailable(): Nothing {
        throw IllegalStateException("Cache directory is unavailable")
    }
}
