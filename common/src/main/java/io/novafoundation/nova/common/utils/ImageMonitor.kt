package io.novafoundation.nova.common.utils

import android.os.FileObserver
import android.widget.ImageView
import coil.ImageLoader
import coil.load
import java.io.File

class ImageMonitor(
    private val imageView: ImageView,
    private val imageLoader: ImageLoader
) {

    private var fileObserver: FileObserver? = null

    fun startMonitoring(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            return
        }

        // Stop watching previous file
        stopMonitoring()

        // Initialize FileObserver to monitor changes to the file
        fileObserver = object : FileObserver(filePath, MODIFY) {
            override fun onEvent(event: Int, path: String?) {
                if (event == MODIFY) {
                    // When file is updated, invalidate the cache and reload
                    reloadImage(filePath)
                }
            }
        }

        fileObserver?.startWatching()

        // Load the initial image
        reloadImage(filePath)
    }

    fun stopMonitoring() {
        fileObserver?.stopWatching()
        fileObserver = null
    }

    private fun reloadImage(filePath: String) {
        imageView.load(File(filePath), imageLoader)
    }
}

fun ImageMonitor.setPathOrStopWatching(filePath: String?) {
    if (filePath == null) {
        stopMonitoring()
    } else {
        startMonitoring(filePath)

    }
}
