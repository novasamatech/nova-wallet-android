package io.novafoundation.nova.common.view.paralaxCard

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader

class BitmapShaderWithPaint(val bitmap: Bitmap) {
    val shader = BitmapShader(
        bitmap,
        Shader.TileMode.CLAMP,
        Shader.TileMode.CLAMP
    )

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        paint.shader = shader
        paint.style = Paint.Style.FILL
    }
}

class BitmapWithRect(val bitmap: Bitmap) {
    val rect: RectF = RectF()
}
