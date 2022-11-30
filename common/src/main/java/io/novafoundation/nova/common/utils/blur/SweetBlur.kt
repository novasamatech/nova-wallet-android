package io.novafoundation.nova.common.utils.blur

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.View
import androidx.core.graphics.toRect
import com.google.android.renderscript.Toolkit
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.core.graphics.times
import kotlin.math.roundToInt

class SweetBlur(
    private val targetView: View,
    private val captureFromView: View,
    private val extraSpace: RectF?,
    private val cutSpace: RectF?,
    private val blurColor: Int?,
    private val onException: ((Exception) -> Unit)?,
    fakeRadius: Int,
) : ViewTreeObserver.OnDrawListener {

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

        fun onException(action: (Exception) -> Unit) = apply { onException = action }

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

    var started: Boolean = false

    val radius: Int
    val downscaleFactor: Float

    val paint: Paint?

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

        paint = if (blurColor != null) {
            Paint().apply {
                flags = Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG
                val filter = PorterDuffColorFilter(blurColor, PorterDuff.Mode.SRC_ATOP)
                colorFilter = filter
            }
        } else {
            null
        }
    }

    // Попробовать написать класс, который будет вычислять возможности устройства и применять бэкграунд попроще, если устройство слабое
    fun start() {
        if (!started) {
            captureFromView.post {
                captureFromView.viewTreeObserver.addOnDrawListener(this)
            }
            started = true
        }
    }

    fun stop() {
        if (started) {
            captureFromView.post {
                captureFromView.viewTreeObserver.removeOnDrawListener(this)
            }
            started = false
        }
    }

    override fun onDraw() {
        if (!started) return

        try {
            makeBlurBackground()
        } catch (e: Exception) {
            stop()
            onException?.invoke(e)
        }
    }

    private fun makeBlurBackground() {
        val bitmap = captureBitmap()
        var blur = blurBitmap(bitmap)

        if (cutSpace != null) {
            val blurRect = blur.rect()
                .inset(cutSpace * downscaleFactor)
            blur = createBitmap(
                blur,
                blurRect
            )
        }

        targetView.background = BitmapDrawable(targetView.context.resources, blur)
    }

    private fun captureBitmap(): Bitmap {
        val capturedBitmap = bitmapFromCaptureView()
        return if (paint != null) {
            colorizeBitmap(capturedBitmap, paint)
        } else {
            capturedBitmap
        }
    }

    private fun getViewClip(): RectF {
        var clip = targetView.getClip()

        if (extraSpace != null) {
            clip = clip.extra(extraSpace)
        }

        return clip
    }

    private fun getTargetSizeBitmap(viewClip: RectF): Bitmap {
        return Bitmap.createBitmap(
            (viewClip.width() * downscaleFactor).toInt(),
            (viewClip.height() * downscaleFactor).toInt(),
            Bitmap.Config.ARGB_8888
        )
    }

    private fun blurBitmap(src: Bitmap): Bitmap {
        return Toolkit.blur(src, radius)
    }

    private fun bitmapFromCaptureView(): Bitmap {
        val viewClip = getViewClip()
        val targetBitmap = getTargetSizeBitmap(viewClip)
        val canvas = Canvas(targetBitmap)
        val matrix = Matrix()
        matrix.setTranslate(0f, -viewClip.top * downscaleFactor)
        matrix.preScale(downscaleFactor, downscaleFactor)
        canvas.setMatrix(matrix)

        captureFromView.draw(canvas)
        return targetBitmap
    }

    private fun colorizeBitmap(bitmap: Bitmap, paint: Paint): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, Matrix(), paint)
        return result
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
}
