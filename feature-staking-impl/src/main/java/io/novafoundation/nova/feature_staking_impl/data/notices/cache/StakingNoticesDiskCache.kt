package io.novafoundation.nova.feature_staking_impl.data.notices.cache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class StakingNoticesDiskCache(private val context: Context) {

    private val cacheFile: File
        get() = File(context.cacheDir, FILE_NAME)

    suspend fun read(): String? = withContext(Dispatchers.IO) {
        runCatching { cacheFile.takeIf { it.exists() }?.readText() }.getOrNull()
    }

    suspend fun write(rawJson: String) = withContext(Dispatchers.IO) {
        runCatching {
            val tmp = File(context.cacheDir, "$FILE_NAME.tmp")
            tmp.writeText(rawJson)
            tmp.renameTo(cacheFile)
        }
        Unit
    }

    companion object {
        private const val FILE_NAME = "staking_notices.json"
    }
}
