package io.novafoundation.nova.common.view.shape

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.StateListDrawable
import android.util.StateSet
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import io.novafoundation.nova.common.R

const val DEFAULT_CORNER_RADIUS = 12

fun Int.toColorStateList() = ColorStateList.valueOf(this)

fun Context.getRippleMask(cornerSizeDp: Int = DEFAULT_CORNER_RADIUS): Drawable {
    return getRoundedCornerDrawableFromColors(Color.WHITE, null, cornerSizeDp)
}

fun Context.getMaskedRipple(
    cornerSizeInDp: Int,
    @ColorInt rippleColor: Int = getColor(R.color.cell_background_pressed)
): Drawable {
    return RippleDrawable(rippleColor.toColorStateList(), null, getRippleMask(cornerSizeInDp))
}

fun Context.addRipple(
    drawable: Drawable? = null,
    mask: Drawable? = getRippleMask(),
    @ColorInt rippleColor: Int = getColor(R.color.cell_background_pressed)
): Drawable {
    return RippleDrawable(rippleColor.toColorStateList(), drawable, mask)
}

fun Context.getCornersStateDrawable(
    disabledDrawable: Drawable = getDisabledDrawable(),
    focusedDrawable: Drawable = getFocusedDrawable(),
    idleDrawable: Drawable = getIdleDrawable(),
): Drawable {
    return StateListDrawable().apply {
        addState(intArrayOf(-android.R.attr.state_enabled), disabledDrawable)
        addState(intArrayOf(android.R.attr.state_focused), focusedDrawable)
        addState(StateSet.WILD_CARD, idleDrawable)
    }
}

fun Context.getCornersCheckableDrawable(
    checked: Drawable,
    unchecked: Drawable
): Drawable {
    return StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_checked), checked)
        addState(StateSet.WILD_CARD, unchecked)
    }
}

fun Context.getInputBackground() = getCornersStateDrawable(
    focusedDrawable = getRoundedCornerDrawableFromColors(
        fillColor = getColor(R.color.input_background),
        strokeColor = getColor(R.color.active_border),
        strokeSizeInDp = 1f
    ),
    idleDrawable = getRoundedCornerDrawable(R.color.input_background)
)

fun Context.getInputBackgroundError(): Drawable {
    val background = getRoundedCornerDrawable(
        fillColorRes = R.color.input_background,
        strokeColorRes = R.color.error_border,
        strokeSizeInDp = 1f
    )

    return getCornersStateDrawable(
        focusedDrawable = background,
        idleDrawable = background
    )
}

fun Context.getFocusedDrawable(): Drawable = getRoundedCornerDrawable(strokeColorRes = R.color.active_border)
fun Context.getDisabledDrawable(): Drawable = getRoundedCornerDrawable(fillColorRes = R.color.input_background)
fun Context.getIdleDrawable(): Drawable = getRoundedCornerDrawable(strokeColorRes = R.color.container_border)
fun Context.getBlockDrawable(@ColorRes strokeColorRes: Int? = null): Drawable {
    return getRoundedCornerDrawable(fillColorRes = R.color.block_background, strokeColorRes = strokeColorRes)
}

fun Context.getRoundedCornerDrawable(
    @ColorRes fillColorRes: Int? = R.color.secondary_screen_background,
    @ColorRes strokeColorRes: Int? = null,
    cornerSizeInDp: Int = DEFAULT_CORNER_RADIUS,
    strokeSizeInDp: Float = 1.0f,
): Drawable {
    val fillColor = fillColorRes?.let(this::getColor)
    val strokeColor = strokeColorRes?.let(this::getColor)

    return getRoundedCornerDrawableFromColors(fillColor, strokeColor, cornerSizeInDp, strokeSizeInDp)
}

fun Context.getTopRoundedCornerDrawable(
    @ColorRes fillColorRes: Int = R.color.secondary_screen_background,
    @ColorRes strokeColorRes: Int? = null,
    cornerSizeInDp: Int = DEFAULT_CORNER_RADIUS,
    strokeSizeInDp: Float = 1.0f,
): Drawable {
    val fillColor = getColor(fillColorRes)
    val strokeColor = strokeColorRes?.let(this::getColor)

    return getTopRoundedCornerDrawableFromColors(fillColor, strokeColor, cornerSizeInDp, strokeSizeInDp)
}

fun Context.getTopRoundedCornerDrawableFromColors(
    @ColorInt fillColor: Int = getColor(R.color.secondary_screen_background),
    @ColorInt strokeColor: Int? = null,
    cornerSizeInDp: Int = DEFAULT_CORNER_RADIUS,
    strokeSizeInDp: Float = 1.0f,
): Drawable {
    return cornerDrawableFromColors(
        fillColor = fillColor,
        strokeColor = strokeColor,
        cornerSizeInDp = cornerSizeInDp,
        strokeSizeInDp = strokeSizeInDp,
        shapeBuilder = { cornerSizePx ->
            ShapeAppearanceModel.Builder()
                .setTopLeftCorner(CornerFamily.ROUNDED, cornerSizePx)
                .setTopRightCorner(CornerFamily.ROUNDED, cornerSizePx)
                .build()
        }
    )
}

fun Context.getRoundedCornerDrawableFromColors(
    @ColorInt fillColor: Int? = getColor(R.color.secondary_screen_background),
    @ColorInt strokeColor: Int? = null,
    cornerSizeInDp: Int = DEFAULT_CORNER_RADIUS,
    strokeSizeInDp: Float = 1.0f,
): Drawable {
    return cornerDrawableFromColors(
        fillColor = fillColor,
        strokeColor = strokeColor,
        cornerSizeInDp = cornerSizeInDp,
        strokeSizeInDp = strokeSizeInDp,
        shapeBuilder = { cornerSizePx ->
            ShapeAppearanceModel.Builder()
                .setAllCorners(CornerFamily.ROUNDED, cornerSizePx)
                .build()
        }
    )
}

private fun Context.cornerDrawableFromColors(
    @ColorInt fillColor: Int?,
    @ColorInt strokeColor: Int?,
    cornerSizeInDp: Int,
    strokeSizeInDp: Float,
    shapeBuilder: (cornerSize: Float) -> ShapeAppearanceModel
): Drawable {
    val density = resources.displayMetrics.density

    val cornerSizePx = density * cornerSizeInDp
    val strokeSizePx = density * strokeSizeInDp

    return MaterialShapeDrawable(shapeBuilder(cornerSizePx)).apply {
        setFillColor(ColorStateList.valueOf(fillColor ?: Color.TRANSPARENT))

        strokeColor?.let {
            setStroke(strokeSizePx, it)
        }
    }
}

fun ovalDrawable(@ColorInt fillColor: Int): Drawable {
    return ShapeDrawable().apply {
        paint.color = fillColor
        shape = android.graphics.drawable.shapes.OvalShape()
    }
}
