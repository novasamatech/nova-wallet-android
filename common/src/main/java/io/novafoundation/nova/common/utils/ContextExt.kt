package io.novafoundation.nova.common.utils

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRippleMask
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable

fun Context.getDrawableCompat(@DrawableRes drawableRes: Int) =
    ContextCompat.getDrawable(this, drawableRes)!!

fun Context.shareText(text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"

        putExtra(Intent.EXTRA_TEXT, text)
    }

    startActivity(intent)
}

inline fun postToUiThread(crossinline action: () -> Unit) {
    Handler(Looper.getMainLooper()).post {
        action.invoke()
    }
}

fun Int.dp(context: Context): Int {
    return dpF(context).toInt()
}

fun Int.dpF(context: Context): Float {
    return toFloat().dpF(context)
}

fun Float.dp(context: Context): Int {
    return dpF(context).toInt()
}

fun Float.dpF(context: Context): Float {
    return context.resources.displayMetrics.density * this
}

fun Context.readAssetFile(name: String) = assets.open(name).readText()

@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

@ColorInt
fun Context.getPrimaryColor() = getColorFromAttr(R.attr.colorPrimary)

@ColorInt
fun Context.getAccentColor() = getColorFromAttr(R.attr.colorAccent)

@ColorRes
fun Context.getColorResFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.resourceId
}

@ColorRes
fun Context.getAccentColorRes() = getColorResFromAttr(R.attr.colorAccent)

fun Context.themed(@StyleRes themeId: Int): Context = ContextThemeWrapper(this, themeId)

interface WithContextExtensions {

    val providedContext: Context

    val Int.dp: Int
        get() = dp(providedContext)

    val Float.dp: Int
        get() = dp(providedContext)

    fun addRipple(to: Drawable, mask: Drawable? = getRippleMask()) = providedContext.addRipple(to, mask)
    fun Drawable.withRippleMask(mask: Drawable = getRippleMask()) = addRipple(this, mask)

    fun getRoundedCornerDrawable(
        @ColorRes fillColorRes: Int = R.color.secondary_screen_background,
        @ColorRes strokeColorRes: Int? = null,
        cornerSizeDp: Int = 12,
    ) = providedContext.getRoundedCornerDrawable(fillColorRes, strokeColorRes, cornerSizeDp)

    fun getRippleMask(
        cornerSizeDp: Int = 12,
    ) = providedContext.getRippleMask(cornerSizeDp)
}

fun WithContextExtensions(context: Context) = object : WithContextExtensions {
    override val providedContext: Context = context
}
