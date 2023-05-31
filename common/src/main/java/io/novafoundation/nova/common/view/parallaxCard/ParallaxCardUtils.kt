package io.novafoundation.nova.common.view.parallaxCard

import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.StyleableRes
import androidx.core.content.res.getResourceIdOrThrow
import com.google.android.renderscript.Toolkit

fun Path.applyRoundRect(rectF: RectF, radius: Float) {
    reset()
    addRoundRect(rectF, radius, radius, Path.Direction.CW)
    close()
}

fun Path.applyRoundRect(view: View, radius: Float) {
    reset()
    addRoundRect(
        view.left.toFloat() + view.translationX,
        view.top.toFloat(),
        view.right.toFloat() + view.translationX,
        view.bottom.toFloat(),
        radius,
        radius,
        Path.Direction.CW
    )
    close()
}

fun RectF.setCardBounds(view: View) {
    set(
        0f,
        0f,
        view.width.toFloat(),
        view.height.toFloat()
    )
}

fun Bitmap.convertToAlphaMask(): Bitmap {
    val alphaMask = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ALPHA_8
    )
    val canvas = Canvas(alphaMask)
    canvas.drawBitmap(this, 0.0f, 0.0f, null)
    return alphaMask
}

fun Bitmap.blurBitmap(size: Int): Bitmap {
    if (size == 0) return this
    return Toolkit.blur(this, size)
}

fun Bitmap.downscale(factor: Float): Bitmap {
    val newBitmap = Bitmap.createBitmap(
        (width * factor).toInt(),
        (height * factor).toInt(),
        config
    )
    val canvas = Canvas(newBitmap)
    canvas.clipRect(0f, 0f, width * factor, height * factor)
    val matrix = Matrix()
    matrix.preScale(factor, factor)
    canvas.setMatrix(matrix)
    canvas.drawBitmap(this, 0f, 0f, null)
    return newBitmap
}

fun Bitmap.toBitmapShaderHelper(): BitmapShaderHelper {
    return BitmapShaderHelper(this)
}

fun Bitmap.toBitmapWithRect(): BitmapWithRect {
    return BitmapWithRect(this)
}

@IdRes
fun TypedArray.getResourceOrNull(@StyleableRes resId: Int): Int? {
    return try {
        getResourceIdOrThrow(resId)
    } catch (e: IllegalArgumentException) {
        null
    }
}

@ColorInt
fun TypedArray.getColorOrNull(@StyleableRes resId: Int): Int? {
    return getResourceOrNull(resId)?.let { getColor(resId, 0) }
}
