package io.novafoundation.nova.feature_dapp_impl.utils.tabs

import android.graphics.Bitmap
import androidx.core.view.drawToBitmap
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTabSession
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSnapshot
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.fromName
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface PageSnapshotBuilder {

    fun getPageSnapshot(browserTabSession: BrowserTabSession): PageSnapshot
}

class RealPageSnapshotBuilder(
    private val fileProvider: FileProvider,
    private val rootScope: RootScope
) : PageSnapshotBuilder {

    override fun getPageSnapshot(browserTabSession: BrowserTabSession): PageSnapshot {
        val webView = browserTabSession.webView
        if (!webView.isLaidOut) return PageSnapshot.fromName(browserTabSession.startUrl)

        val pageName = webView.title
        val icon = webView.favicon
        val pageBitmap = webView.drawToBitmap()

        val pageIconPath = saveBitmap(browserTabSession, icon, "icon", 100)
        val pagePicturePath = saveBitmap(browserTabSession, pageBitmap, "page", 40)

        return PageSnapshot(
            pageName = pageName,
            pageIconPath = pageIconPath,
            pagePicturePath = pagePicturePath
        )
    }

    private fun saveBitmap(browserTabSession: BrowserTabSession, bitmap: Bitmap?, filePrefix: String, quality: Int): String? {
        if (bitmap == null) return null

        // Use this pattern to don't create a new image everytime when we rewrite the page snapshot
        val fileName = "tab_${browserTabSession.tabId}_$filePrefix.jpeg"
        val file = fileProvider.getFileInExternalCacheStorage(fileName)

        try {
            rootScope.launch(Dispatchers.IO) {
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                outputStream.close()
            }

            return file.absolutePath
        } catch (e: IOException) {
            return null
        }
    }
}
