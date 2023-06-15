package io.novafoundation.nova.common.view.parallaxCard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParallaxCardBitmapBaking(private val context: Context, val lruCache: BackingParallaxCardLruCache) {

    private var callback: OnBakingPreparedCallback? = null

    private val dispatcher = Executors.newSingleThreadExecutor()
        .asCoroutineDispatcher() + SupervisorJob()

    val coroutineScope = CoroutineScope(dispatcher)

    // Card background bitmap
    var cardBackgroundBitmap: Bitmap? = null
        private set

    // Highlights bitmaps
    var cardHighlightBitmap: Bitmap? = null
        private set
    var cardBorderHighlightBitmap: Bitmap? = null
        private set
    var nestedViewBorderHighlightBitmap: Bitmap? = null
        private set
    var parallaxHighlightBitmap: Bitmap? = null
        private set

    // Paralax bitmaps
    var parallaxFirstBitmap: BitmapWithRect? = null
    var parallaxSecondBitmap: BitmapWithRect? = null
    var parallaxThirdBitmap: BitmapWithRect? = null

    // Paralax bitmaps blurred
    var parallaxFirstBlurredBitmap: BitmapWithRect? = null
    var parallaxSecondBlurredBitmap: BitmapWithRect? = null
    var parallaxThirdBlurredBitmap: BitmapWithRect? = null

    // Shaders
    var cardHighlightShader: BitmapShaderHelper? = null
    var cardBorderHighlightShader: BitmapShaderHelper? = null
    var parallaxHighlightShader: BitmapShaderHelper? = null
    var nestedViewBorderHighlightShader: BitmapShaderHelper? = null

    var isPrepared: Boolean = false

    init {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    cardBackgroundBitmap = getBitmapFromCache("cardBackgroundBitmap") {
                        BitmapFactory.decodeResource(context.resources, R.drawable.ic_parallax_card_background)
                    }
                    cardHighlightBitmap = getBitmapFromCache("cardHighlightBitmap") {
                        BitmapFactory.decodeResource(context.resources, R.drawable.ic_card_background_highlight).downscale(0.5f)
                    }
                    cardBorderHighlightBitmap = getBitmapFromCache("cardBorderHighlightBitmap") {
                        BitmapFactory.decodeResource(context.resources, R.drawable.ic_card_border_highlight).downscale(0.5f)
                    }
                    nestedViewBorderHighlightBitmap = getBitmapFromCache("nestedViewBorderHighlightBitmap") {
                        BitmapFactory.decodeResource(context.resources, R.drawable.ic_frosted_glass_highlight).downscale(0.5f)
                    }
                    parallaxHighlightBitmap = getBitmapFromCache("parallaxHighlightBitmap") {
                        BitmapFactory.decodeResource(context.resources, R.drawable.ic_pattern_highlight).downscale(0.5f)
                    }

                    parallaxFirstBitmap = getBitmapFromCache("parallaxFirstBitmap") {
                        BitmapFactory.decodeResource(context.resources, R.drawable.ic_big_star)
                            .convertToAlphaMask()
                    }.toBitmapWithRect()
                    parallaxSecondBitmap = getBitmapFromCache("parallaxSecondBitmap") {
                        BitmapFactory.decodeResource(context.resources, R.drawable.ic_middle_star)
                            .blurBitmap(2.dp(context))
                            .convertToAlphaMask()
                    }.toBitmapWithRect()
                    parallaxThirdBitmap = getBitmapFromCache("parallaxThirdBitmap") {
                        BitmapFactory.decodeResource(context.resources, R.drawable.ic_small_star)
                            .blurBitmap(3.dp(context))
                            .convertToAlphaMask()
                    }.toBitmapWithRect()

                    parallaxFirstBlurredBitmap = getBitmapFromCache("parallaxFirstBitmap_blurred") {
                        parallaxFirstBitmap!!.bitmap
                            .downscale(0.25f)
                            .blurBitmap(2.dp(context))
                            .convertToAlphaMask()
                    }.toBitmapWithRect()
                    parallaxSecondBlurredBitmap = getBitmapFromCache("parallaxSecondBitmap_blurred") {
                        parallaxSecondBitmap!!.bitmap
                            .downscale(0.25f)
                            .blurBitmap(2.dp(context))
                            .convertToAlphaMask()
                    }.toBitmapWithRect()
                    parallaxThirdBlurredBitmap = getBitmapFromCache("parallaxThirdBitmap_blurred") {
                        parallaxThirdBitmap!!.bitmap
                            .downscale(0.25f)
                            .blurBitmap(2.dp(context))
                            .convertToAlphaMask()
                    }.toBitmapWithRect()

                    cardHighlightShader = cardHighlightBitmap!!.toBitmapShaderHelper()
                    cardBorderHighlightShader = cardBorderHighlightBitmap!!.toBitmapShaderHelper().apply {
                        paint.strokeWidth = 2.dpF(context)
                        paint.style = Paint.Style.STROKE
                    }
                    parallaxHighlightShader = parallaxHighlightBitmap!!.toBitmapShaderHelper()
                    nestedViewBorderHighlightShader = nestedViewBorderHighlightBitmap!!.toBitmapShaderHelper().apply {
                        paint.strokeWidth = 2.dpF(context)
                        paint.style = Paint.Style.STROKE
                    }
                }

                isPrepared = true
                callback?.onBakingPrepared()
            } catch (e: Exception) {
                throw e
            }
        }
    }

    fun onViewRemove() {
        coroutineScope.cancel()
        callback = null
    }

    fun setBakingPreparedCallback(callback: OnBakingPreparedCallback) {
        this.callback = callback
    }

    private fun getBitmapFromCache(key: String, bake: () -> Bitmap): Bitmap {
        var bitmap = lruCache.get(key)
        if (bitmap == null) {
            bitmap = bake()
            lruCache.put(key, bitmap)
        }

        return bitmap
    }

    interface OnBakingPreparedCallback {
        fun onBakingPrepared()
    }
}
