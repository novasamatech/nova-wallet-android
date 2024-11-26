package io.novafoundation.nova.common.utils.coil.transformation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import coil.bitmap.BitmapPool
import coil.size.Size
import coil.size.PixelSize
import coil.transform.Transformation

class TopCropTransformation : Transformation {

    override fun key(): String {
        return "TopCropTransformation"
    }

    override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap {
        val (width, height) = if (size is PixelSize) {
            size.width to size.height
        } else {
            input.width to input.height
        }

        val scale = width / input.width.toFloat()

        val targetWidth = (input.width * scale).toInt()
        val targetHeight = height

        val output = Bitmap.createBitmap(targetWidth, targetHeight, input.config)

        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val matrix = Matrix()
        matrix.setScale(scale, scale)
        matrix.postTranslate(0f, 0f)

        canvas.drawBitmap(input, matrix, paint)

        return output
    }
}
