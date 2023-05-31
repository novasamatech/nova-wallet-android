package io.novafoundation.nova.common.view.parallaxCard

import android.graphics.Bitmap
import android.util.LruCache

private const val DISK_CACHE_SIZE = 1024 * 1024 // 1MB

class BackingParallaxCardLruCache(cacheSizeInMb: Int) : LruCache<String, Bitmap>(cacheSizeInMb * DISK_CACHE_SIZE) {

    override fun sizeOf(key: String, bitmap: Bitmap): Int {
        return bitmap.byteCount / 1024
    }
}
