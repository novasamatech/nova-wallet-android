package io.novafoundation.nova.common.interfaces

import android.net.Uri
import java.io.File

interface FileProvider {

    fun getFileInExternalCacheStorage(fileName: String): File

    fun getFileInInternalCacheStorage(fileName: String): File

    fun generateTempFile(fixedName: String? = null): File

    fun uriOf(file: File): Uri
}
