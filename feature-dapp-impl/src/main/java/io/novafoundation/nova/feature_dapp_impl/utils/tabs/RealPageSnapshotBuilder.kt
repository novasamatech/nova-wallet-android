package io.novafoundation.nova.feature_dapp_impl.utils.tabs

import android.graphics.Bitmap
import androidx.core.view.drawToBitmap
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTabSession
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSnapshot
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PageSnapshotBuilder {

    suspend fun getPageSnapshot(browserTabSession: BrowserTabSession): PageSnapshot
}

class RealPageSnapshotBuilder(
    private val fileProvider: FileProvider
) : PageSnapshotBuilder {

    override suspend fun getPageSnapshot(browserTabSession: BrowserTabSession): PageSnapshot {
        val webView = browserTabSession.webView
        val pageName = webView.title
        val icon = webView.favicon
        val pageBitmap = webView.drawToBitmap()

        val pageIconPath = saveBitmap(browserTabSession, icon, "icon")
        val pagePicturePath = saveBitmap(browserTabSession, pageBitmap, "page")

        return PageSnapshot(
            pageName = pageName,
            pageIconPath = pageIconPath,
            pagePicturePath = pagePicturePath
        )
    }

    private suspend fun saveBitmap(browserTabSession: BrowserTabSession, bitmap: Bitmap?, filePrefix: String): String? {
        if (bitmap == null) return null

        // Use this pattern to don't create a new image everytime when we rewrite the page snapshot
        val fileName = "tab_${browserTabSession.tabId}_$filePrefix.jpeg"
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
