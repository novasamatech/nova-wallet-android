package io.novafoundation.nova.feature_dapp_impl.utils.tabs

import android.graphics.Bitmap
import androidx.core.view.drawToBitmap
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSession
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSnapshot
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PageSnapshotBuilder(
    private val fileProvider: FileProvider
) {

    suspend fun getPageSnapshot(pageSession: PageSession): PageSnapshot {
        val webView = pageSession.webView
        val pageName = webView.title
        val icon = webView.favicon
        val pageBitmap = webView.drawToBitmap()

        val pageIconPath = saveBitmap(pageSession, icon, "icon")
        val pagePicturePath = saveBitmap(pageSession, pageBitmap, "page")

        return PageSnapshot(
            pageName = pageName,
            pageIconPath = pagePicturePath,
            pagePicturePath = pageIconPath
        )
    }

    private suspend fun saveBitmap(pageSession: PageSession, bitmap: Bitmap?, filePrefix: String): String? {
        if (bitmap == null) return null

        // Use this pattern to don't create a new image everytime when we rewrite the page snapshot
        val fileName = "tab_${pageSession.tabId}_$filePrefix.jpeg"
        val file = fileProvider.getFileInExternalCacheStorage(fileName)

        try {
            withContext(Dispatchers.IO) {
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
            }

            return file.absolutePath
        } catch (e: IOException) {
            return null
        }
    }
}
