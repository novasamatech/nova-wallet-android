package io.novafoundation.nova.common.utils.blur

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.core.graphics.times
import androidx.core.graphics.toRect
import com.google.android.renderscript.Toolkit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

class SweetBlur(
    private val targetView: View,
    private val captureFromView: View,
    private val extraSpace: RectF?,
    private val cutSpace: RectF?,
    private val blurColor: Int?,
    private val onException: ((Exception) -> Unit)?,
    fakeRadius: Int,
) : ViewTreeObserver.OnDrawListener, CoroutineScope {

    class ViewBackgroundBuilder {

        private var targetView: View? = null
        private var captureFromView: View? = null
        private var extraSpace: RectF? = null
        private var cutSpace: RectF? = null
        private var radius: Int? = null
        private var blurColor: Int? = null
        private var onException: ((Exception) -> Unit)? = null

        fun toTarget(targetView: View) = apply { this.targetView = targetView }

        fun captureFrom(captureFromView: View) = apply { this.captureFromView = captureFromView }

        fun captureExtraSpace(extraSpace: RectF) = apply { this.extraSpace = extraSpace }

        fun cutSpace(cutSpace: RectF) = apply { this.cutSpace = cutSpace }

        fun radius(radius: Int) = apply { this.radius = radius }

        fun blurColor(@ColorInt blurColor: Int) = apply { this.blurColor = blurColor }

        fun catchException(action: (Exception) -> Unit) = apply { onException = action }

        fun build(): SweetBlur {
            return SweetBlur(
                targetView!!,
                captureFromView!!,
                extraSpace,
                cutSpace,
                blurColor,
                onException,
                radius!!,
            )
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    var started: Boolean = false
    val radius: Int
    private val downscaleFactor: Float
    private val bitmapColorFilter: ColorFilter?

    init {
        val boundedFakeRadius = minOf(maxOf(fakeRadius, 1), 250)
        val radiusRange = 249f
        val trueRadiusRange = 24f
        val compressionRange = 9f
        val decelerate = DecelerateInterpolator(3f)
        val accelerate = AccelerateInterpolator(0.5f)
        val compression = 1f + accelerate.getInterpolation((boundedFakeRadius - 1) / radiusRange) * compressionRange
        val trueRadius = 1f + decelerate.getInterpolation((boundedFakeRadius - 1) / radiusRange) * trueRadiusRange
        downscaleFactor = 1f / compression
        radius = trueRadius.roundToInt()

        bitmapColorFilter = if (blurColor == null) null else PorterDuffColorFilter(blurColor, PorterDuff.Mode.SRC_ATOP)
    }

    fun start() {
        if (!started) {
            started = true
            captureFromView.post {
                captureFromView.viewTreeObserver.addOnDrawListener(this)
            }
        }
    }

    fun stop() {
        if (started) {
            started = false
            captureFromView.post {
                captureFromView.viewTreeObserver.removeOnDrawListener(this)
            }
        }
    }

    override fun onDraw() {
        if (!started) return

        targetView.post {
            try {
                makeBlurBackground()
            } catch (e: Exception) {
                stop()
                onException?.invoke(e) ?: throw e
            }
        }
    }

    private fun makeBlurBackground() {
        val capturedBitmap = captureBitmap() ?: return
        launch {
            try {
                val bitmapDrawable = withContext(Dispatchers.Default) {
                    val blurBitmap = blurBitmap(capturedBitmap)
                    val cutSpaceBlur = applyCutSpace(blurBitmap)
                    createBlurDrawable(cutSpaceBlur)
                }
                targetView.background = bitmapDrawable
            } catch (e: Exception) {
                stop()
                onException?.invoke(e) ?: throw e
            }
        }
    }

    private fun applyCutSpace(bitmap: Bitmap): Bitmap {
        return if (cutSpace != null) {
            val blurRect = bitmap.rect()
                .inset(cutSpace * downscaleFactor)
            return createBitmap(
                bitmap,
                blurRect
            )
        } else {
            bitmap
        }
    }

    private fun getViewClip(): RectF {
        var clip = targetView.getClip()

        if (extraSpace != null) {
            clip = clip.extra(extraSpace)
        }

        return clip
    }

    private fun getTargetSizeBitmap(viewClip: RectF): Bitmap? {
        val width = (viewClip.width() * downscaleFactor).toInt()
        val height = (viewClip.height() * downscaleFactor).toInt()
        if (width <= 0 || height <= 0) return null

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    fun blurBitmap(src: Bitmap): Bitmap {
        return Toolkit.blur(src, radius)
    }

    private fun captureBitmap(): Bitmap? {
        val viewClip = getViewClip()

        val targetBitmap = getTargetSizeBitmap(viewClip) ?: return null
        targetBitmap.eraseColor(Color.BLACK)
        val canvas = SweetBlurCanvas(targetBitmap)
        canvas.clipRect(0f, 0f, viewClip.width() * downscaleFactor, viewClip.height() * downscaleFactor)
        val matrix = Matrix()
        matrix.setTranslate(0f, -viewClip.top * downscaleFactor)
        matrix.preScale(downscaleFactor, downscaleFactor)
        canvas.setMatrix(matrix)
        captureFromView.draw(canvas)
        return targetBitmap
    }

    private fun createBitmap(source: Bitmap, clipF: RectF): Bitmap {
        val clip = clipF.toRect()
        return Bitmap.createBitmap(source, clip.left, clip.top, clip.width(), clip.height())
    }

    private fun Bitmap.rect(): RectF {
        return RectF(0f, 0f, width.toFloat(), height.toFloat())
    }

    private fun RectF.extra(other: RectF): RectF {
        return RectF(
            left - other.left,
            top - other.top,
            right + other.right,
            bottom + other.bottom
        )
    }

    private fun RectF.inset(other: RectF): RectF {
        return RectF(
            left + other.left,
            top + other.top,
            right - other.right,
            bottom - other.bottom
        )
    }

    private fun View.getClip(): RectF {
        return RectF(
            left.toFloat(),
            top.toFloat(),
            right.toFloat(),
            bottom.toFloat()
        )
    }

    private fun createBlurDrawable(bitmap: Bitmap): BitmapDrawable {
        return BitmapDrawable(targetView.context.resources, bitmap).apply {
            colorFilter = bitmapColorFilter
        }
    }
}
