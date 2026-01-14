package io.novafoundation.nova.common.view.parallaxCard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.util.Log
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.data.network.TAG
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.forEachAsync
import io.novafoundation.nova.common.utils.mapIf
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParallaxCardResourceManager(
    context: Context,
    val parallaxLayers: List<ParallaxLayer>,
    val lruCache: BackingParallaxCardLruCache
) {

    private var callback: OnBakingPreparedCallback? = null

    private val dispatcher = Executors.newSingleThreadExecutor()
        .asCoroutineDispatcher() + SupervisorJob()

    val coroutineScope = CoroutineScope(dispatcher)

    val cardBackgroundBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_parallax_card_background)
    val cardHighlightBitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_card_background_highlight)
    val cardBorderHighlightBitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_card_border_highlight)
    val nestedViewBorderHighlightBitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_frosted_glass_highlight)
    val parallaxHighlightBitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_pattern_highlight)

    // Shaders
    var cardHighlightShader: BitmapShaderHelper = cardHighlightBitmap.toBitmapShaderHelper()
    var cardBorderHighlightShader: BitmapShaderHelper = cardBorderHighlightBitmap.toBitmapShaderHelper()
        .apply {
            paint.strokeWidth = 2.dpF(context)
            paint.style = Paint.Style.STROKE
        }
    var parallaxHighlightShader: BitmapShaderHelper = parallaxHighlightBitmap.toBitmapShaderHelper()
    var nestedViewBorderHighlightShader: BitmapShaderHelper = nestedViewBorderHighlightBitmap.toBitmapShaderHelper()
        .apply {
            paint.strokeWidth = 2.dpF(context)
            paint.style = Paint.Style.STROKE
        }

    var isPrepared: Boolean = false

    init {
        parallaxLayers.forEach {
            val layer = getBitmapFromCache(it.id)
            val blurredLayer = getBitmapFromCache(blurredKey(it.id))
            if (layer != null && blurredLayer != null) {
                it.onReady(layer, blurredLayer)
            }
        }

        val layersToBake = parallaxLayers.filter { it.isNotReady() }

        if (layersToBake.isEmpty()) {
            onPrepared()
        } else {
            coroutineScope.launch {
                bakeParallaxLayers(context, layersToBake)
            }
        }
    }

    private suspend fun bakeParallaxLayers(context: Context, layersToBake: List<ParallaxLayer>) = runCatching {
        withContext(Dispatchers.Default) {
            layersToBake.filter { it.isNotReady() }
                .forEachAsync {
                    val layer = getBitmapFromCache(it.id) {
                        BitmapFactory.decodeResource(context.resources, it.bitmapId)
                            .mapIf(it.blurRadius > 0) { blurBitmap(it.blurRadius) }
                            .mapIf(it.withHighlighting) { convertToAlphaMask() }
                    }
                    val layerBlurred = getBitmapFromCache(blurredKey(it.id)) {
                        layer.downscale(0.25f)
                            .blurBitmap(2.dp(context))
                            .mapIf(it.withHighlighting) { convertToAlphaMask() }
                    }

                    it.onReady(layer, layerBlurred)
                }
        }

        onPrepared()
    }.onFailure {
        Log.d(TAG, it.message, it)
    }

    fun onViewRemove() {
        coroutineScope.cancel()
        callback = null
    }

    fun setBakingPreparedCallback(callback: OnBakingPreparedCallback) {
        this.callback = callback
    }

    private fun onPrepared() {
        isPrepared = true
        callback?.onBakingPrepared()
    }

    private fun getBitmapFromCache(key: String): Bitmap? {
        return lruCache.get(key)
    }

    private fun getBitmapFromCache(key: String, bake: () -> Bitmap): Bitmap {
        var bitmap = lruCache.get(key)
        if (bitmap == null) {
            bitmap = bake()
            lruCache.put(key, bitmap)
        }

        return bitmap
    }

    private fun blurredKey(key: String) = "${key}_blurred"

    interface OnBakingPreparedCallback {
        fun onBakingPrepared()
    }
}
