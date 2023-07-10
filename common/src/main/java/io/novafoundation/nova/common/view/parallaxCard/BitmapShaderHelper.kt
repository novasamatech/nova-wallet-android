package io.novafoundation.nova.common.view.parallaxCard

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader

class BitmapShaderHelper(val bitmap: Bitmap) {

    val shader = BitmapShader(
        bitmap,
        Shader.TileMode.CLAMP,
        Shader.TileMode.CLAMP
    )
    val matrix = Matrix()

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        paint.shader = shader
        paint.style = Paint.Style.FILL
        setAlpha(1f)
    }

    fun setAlpha(alpha: Float) {
        paint.alpha = (alpha * 255).toInt()
    }
}
