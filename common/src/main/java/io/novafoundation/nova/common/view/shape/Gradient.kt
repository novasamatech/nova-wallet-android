package io.novafoundation.nova.common.view.shape

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.quantize

fun Context.gradientDrawable(
    colors: IntArray,
    offsets: FloatArray,
    angle: Int,
    cornerRadiusDp: Int
): Drawable {
    val gradientFactory: ShapeDrawable.ShaderFactory = object : ShapeDrawable.ShaderFactory() {
        override fun resize(width: Int, height: Int): Shader {
            val (x0, y0, x1, y1) = gradientDirectionCoordinates(angle, width.toFloat(), height.toFloat())

            return LinearGradient(x0, y0, x1, y1, colors, offsets, Shader.TileMode.CLAMP)
        }
    }
    val roundCorners = FloatArray(8) { cornerRadiusDp.dpF(this) }

    return ShapeDrawable(RoundRectShape(roundCorners, null, null)).apply {
        shaderFactory = gradientFactory
    }
}

private fun gradientDirectionCoordinates(
    angle: Int,
    width: Float,
    height: Float
): List<Float> {
    val x0: Float
    val x1: Float
    val y0: Float
    val y1: Float

    // Adopted from GradientDrawable since it does not allow to supply positions on pre-Q (<29) devices
    when (angle.quantize(45)) {
        270 -> {
            x0 = 0f
            y0 = 0f
            x1 = x0
            y1 = height
        }
        225 -> {
            x0 = width
            y0 = 0f
            x1 = 0f
            y1 = height
        }
        180 -> {
            x0 = width
            y0 = 0f
            x1 = 0f
            y1 = y0
        }
        135 -> {
            x0 = width
            y0 = height
            x1 = 0f
            y1 = 0f
        }
        90 -> {
            x0 = 0f
            y0 = height
            x1 = x0
            y1 = 0f
        }
        45 -> {
            x0 = 0f
            y0 = height
            x1 = width
            y1 = 0f
        }
        0 -> {
            x0 = 0f
            y0 = 0f
            x1 = width
            y1 = y0
        }
        else -> {
            x0 = 0f
            y0 = 0f
            x1 = width
            y1 = height
        }
    }

    return listOf(x0, y0, x1, y1)
}
