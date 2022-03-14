package io.novafoundation.nova.common.view.shape

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.util.StateSet
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.getAccentColor

fun Int.toColorStateList() = ColorStateList.valueOf(this)

fun Context.addRipple(
    drawable: Drawable? = null,
    mask: Drawable? = null,
    @ColorInt rippleColor: Int = getColor(R.color.colorSelected)
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

fun Context.getInputBackground() = getCornersStateDrawable(
    focusedDrawable = getRoundedCornerDrawableFromColors(
        fillColor = getColor(R.color.white_8),
        strokeColor = getAccentColor(),
    ),
    idleDrawable = getRoundedCornerDrawable(R.color.white_8)
)

fun Context.getFocusedDrawable(): Drawable = getRoundedCornerDrawable(strokeColorRes = R.color.white)
fun Context.getDisabledDrawable(): Drawable = getRoundedCornerDrawable(fillColorRes = R.color.gray3)
fun Context.getIdleDrawable(): Drawable = getRoundedCornerDrawable(strokeColorRes = R.color.gray2)
fun Context.getBlurDrawable(@ColorRes strokeColorRes: Int? = null): Drawable {
    return getRoundedCornerDrawable(fillColorRes = R.color.blurColor, strokeColorRes = strokeColorRes)
}

fun Context.getRoundedCornerDrawable(
    @ColorRes fillColorRes: Int = R.color.black,
    @ColorRes strokeColorRes: Int? = null,
    cornerSizeInDp: Int = 10,
    strokeSizeInDp: Int = 1,
): Drawable {
    val fillColor = getColor(fillColorRes)
    val strokeColor = strokeColorRes?.let(this::getColor)

    return getRoundedCornerDrawableFromColors(fillColor, strokeColor, cornerSizeInDp, strokeSizeInDp)
}

fun Context.getTopRoundedCornerDrawable(
    @ColorRes fillColorRes: Int = R.color.black,
    @ColorRes strokeColorRes: Int? = null,
    cornerSizeInDp: Int = 10,
    strokeSizeInDp: Int = 1,
): Drawable {
    val fillColor = getColor(fillColorRes)
    val strokeColor = strokeColorRes?.let(this::getColor)

    return getTopRoundedCornerDrawableFromColors(fillColor, strokeColor, cornerSizeInDp, strokeSizeInDp)
}

fun Context.getTopRoundedCornerDrawableFromColors(
    @ColorInt fillColor: Int = getColor(R.color.black),
    @ColorInt strokeColor: Int? = null,
    cornerSizeInDp: Int = 10,
    strokeSizeInDp: Int = 1,
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
    @ColorInt fillColor: Int = getColor(R.color.black),
    @ColorInt strokeColor: Int? = null,
    cornerSizeInDp: Int = 10,
    strokeSizeInDp: Int = 1,
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
    @ColorInt fillColor: Int,
    @ColorInt strokeColor: Int?,
    cornerSizeInDp: Int,
    strokeSizeInDp: Int,
    shapeBuilder: (cornerSize: Float) -> ShapeAppearanceModel
): Drawable {
    val density = resources.displayMetrics.density

    val cornerSizePx = density * cornerSizeInDp
    val strokeSizePx = density * strokeSizeInDp

    return MaterialShapeDrawable(shapeBuilder(cornerSizePx)).apply {
        setFillColor(ColorStateList.valueOf(fillColor))

        strokeColor?.let {
            setStroke(strokeSizePx, it)
        }
    }
}
