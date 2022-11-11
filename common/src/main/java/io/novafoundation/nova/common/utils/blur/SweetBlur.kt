package io.novafoundation.nova.common.utils.blur

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.View
import androidx.core.graphics.toRect
import com.google.android.renderscript.Toolkit
import java.lang.RuntimeException
import android.graphics.PorterDuff


import android.graphics.PorterDuffColorFilter
import androidx.core.graphics.times


class SweetBlur {

    fun blurBackground(
        target: View, // view to put background
        captureView: View, // view to capture background
        extraSpace: RectF, // used to capture extra space to make blur smoother
        inset: RectF, // used to cut inner space to avoid
        radius: Int // radius of blur 1 to 250
    ) {
        require(radius in 1..250)
        // radius inscrease from 1 to 25
        // compression increase from 1 to 10
        val radiusRange = 249f
        val trueRadiusRange = 24f
        val compressionRange = 9f
        val trueRadius = 1f + ((radius - 1f) / (radiusRange / trueRadiusRange))
        val compression = 1f + ((radius - 1f) / (radiusRange / compressionRange))

        blurBackground(
            target,
            captureView,
            extraSpace,
            inset,
            trueRadius.toInt(),
            1f / compression
        )
    }

    fun blurBackground(
        target: View, // view to put background
        captureView: View, // view to capture background
        extraSpace: RectF, // used to capture extra space to make blur smoother
        inset: RectF, // used to cut inner space to avoid
        radius: Int, // radius of blur 1 to 25
        downscaleFactor: Float // used to make calculation faster
    ) {
        captureView.viewTreeObserver.addOnDrawListener {
            val viewClip = RectF(
                target.left.toFloat(),
                target.top.toFloat(),
                target.right.toFloat(),
                target.bottom.toFloat()
            )
            val bitmapClip = viewClip.extra(extraSpace)

            val bitmap = getBitmapForView(
                captureView,
                bitmapClip,
                downscaleFactor
            )
            var blur = blur(bitmap, radius)
            var blurRect = blur.rect() // optimize rect
            blurRect = blurRect.inset(extraSpace * downscaleFactor)
            blurRect = blurRect.inset(inset * downscaleFactor)
            blur = createBitmap(
                blur,
                blurRect
            )
            target.background = BitmapDrawable(target.context.resources, blur)
        }
    }

    private fun getBitmapForView(on: View, clip: RectF, downscaleFactor: Float): Bitmap {
        val bitmap = Bitmap.createBitmap(
            (clip.width() * downscaleFactor).toInt(),
            (clip.height() * downscaleFactor).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val matrix = Matrix()
        matrix.setTranslate(0f, -clip.top * downscaleFactor)
        matrix.preScale(downscaleFactor, downscaleFactor)
        canvas.setMatrix(matrix)
        on.draw(canvas)
        val paint = Paint()
        paint.flags = Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG
        val filter = PorterDuffColorFilter(Color.parseColor("#6605071C"), PorterDuff.Mode.SRC_ATOP)
        paint.colorFilter = filter
        val bitmap2 = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas2 = Canvas(bitmap2)
        canvas2.drawBitmap(bitmap, Matrix(), paint)
        return bitmap2
    }

    private fun blur(src: Bitmap, radius: Int): Bitmap {
        return Toolkit.blur(src, radius)
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

    companion object {
        private var instance: SweetBlur? = null
        fun init(context: Context) {
            if (instance != null) {
                return
            }

            instance = SweetBlur()
        }

        fun getInstance(): SweetBlur {
            if (instance == null) {
                throw RuntimeException("BlurKit not initialized!")
            }

            return instance!!
        }
    }
}
