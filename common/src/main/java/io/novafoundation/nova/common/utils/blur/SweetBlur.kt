package io.novafoundation.nova.common.utils.blur

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.renderscript.RenderScript
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.times
import androidx.core.graphics.toRect
import com.google.android.renderscript.Toolkit
import java.lang.RuntimeException
import android.graphics.PorterDuff


import android.graphics.PorterDuffColorFilter


class SweetBlur(private val rs: RenderScript) {

    fun blur(src: Bitmap, radius: Int): Bitmap {
        return Toolkit.blur(src, radius)
        /*val input = Allocation.createFromBitmap(rs, src)
        val output = Allocation.createTyped(rs, input.type)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        script.setRadius(radius.toFloat())
        script.setInput(input)
        script.forEach(output)
        output.copyTo(src)
        return src*/
    }

    fun blurBackground(
        target: View,
        on: View,
        radius: Int,
        downscaleFactor: Float
    ) {
        val parent = target.parent as ViewGroup
        parent.viewTreeObserver.addOnDrawListener {
            val clip = RectF(target.left.toFloat(), target.top.toFloat(), target.right.toFloat(), target.bottom.toFloat())
            val bitmap = getBitmapForView2(
                on,
                clip,
                downscaleFactor
            )
            var blur = blur(bitmap, radius)
            repeat(1) { blur = blur(blur, radius) }
            blur = createBitmap(
                blur,
                clip * downscaleFactor
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

    private fun getBitmapForView2(on: View, clip: RectF, downscaleFactor: Float): Bitmap {
        val bitmap = Bitmap.createBitmap(
            (on.measuredWidth * downscaleFactor).toInt(),
            (on.measuredHeight * downscaleFactor).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val matrix = Matrix()
        //matrix.setTranslate(0f, -clip.top * downscaleFactor)
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

        val clip2 = clip * downscaleFactor
        return bitmap2
    }

    private fun createBitmap(source: Bitmap, clipF: RectF): Bitmap {
        val clip = clipF.toRect()
        return Bitmap.createBitmap(source, clip.left, clip.top, clip.width(), clip.height())
    }

    companion object {
        private var instance: SweetBlur? = null
        fun init(context: Context) {
            if (instance != null) {
                return
            }
            val renderScript = RenderScript.create(context.applicationContext)
            instance = SweetBlur(renderScript)
        }

        fun getInstance(): SweetBlur {
            if (instance == null) {
                throw RuntimeException("BlurKit not initialized!")
            }

            return instance!!
        }
    }
}
