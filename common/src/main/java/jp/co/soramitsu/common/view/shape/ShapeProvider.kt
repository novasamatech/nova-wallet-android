package jp.co.soramitsu.common.view.shape

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
import jp.co.soramitsu.common.R

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

fun Context.getFocusedDrawable(): Drawable = getRoundedCornerDrawable(strokeColorRes = R.color.white)
fun Context.getDisabledDrawable(): Drawable = getRoundedCornerDrawable(fillColorRes = R.color.gray3)
fun Context.getIdleDrawable(): Drawable = getRoundedCornerDrawable(strokeColorRes = R.color.gray2)

fun Context.getCutCornerDrawable(
    @ColorRes fillColorRes: Int = R.color.black,
    @ColorRes strokeColorRes: Int? = null,
    cornerSizeInDp: Int = 10,
    strokeSizeInDp: Int = 1,
): Drawable {
    val fillColor = getColor(fillColorRes)
    val strokeColor = strokeColorRes?.let(this::getColor)

    return getCutCornerDrawableFromColors(fillColor, strokeColor, cornerSizeInDp, strokeSizeInDp)
}

fun Context.getCutCornerDrawableFromColors(
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
                .setTopLeftCorner(CornerFamily.CUT, cornerSizePx)
                .setBottomRightCorner(CornerFamily.CUT, cornerSizePx)
                .build()
        }
    )
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
