package io.novafoundation.nova.common.view.shape

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.ShapeDrawable.ShaderFactory
import android.graphics.drawable.shapes.RoundRectShape
import io.novafoundation.nova.common.utils.dpF

fun Context.gradientDrawable(
    colors: IntArray,
    offsets: FloatArray,
    cornerRadiusDp: Int
): Drawable {
    val gradientFactory: ShaderFactory = object : ShaderFactory() {
        override fun resize(width: Int, height: Int): Shader {
            return LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), colors, offsets, Shader.TileMode.CLAMP)
        }
    }
    val roundCorners = FloatArray(8) { cornerRadiusDp.dpF(this) }

    return ShapeDrawable(RoundRectShape(roundCorners, null, null)).apply {
        shaderFactory = gradientFactory
    }
}
