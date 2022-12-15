package io.novafoundation.nova.common.utils.blur

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build

class SweetBlurCanvas(bitmap: Bitmap) : Canvas(bitmap) {
    override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: RectF, paint: Paint?) {
        super.drawBitmap(makeSoftwareIfHardware(bitmap), src, dst, paint)
    }

    override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: Rect, paint: Paint?) {
        super.drawBitmap(makeSoftwareIfHardware(bitmap), src, dst, paint)
    }

    override fun drawBitmap(bitmap: Bitmap, matrix: Matrix, paint: Paint?) {
        super.drawBitmap(makeSoftwareIfHardware(bitmap), matrix, paint)
    }

    override fun drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) {
        super.drawBitmap(makeSoftwareIfHardware(bitmap), left, top, paint)
    }

    private fun makeSoftwareIfHardware(bitmap: Bitmap): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, bitmap.isMutable)
        } else {
            bitmap
        }
    }
}
