package io.novafoundation.nova.common.presentation.masking

import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.drawableText
import io.novafoundation.nova.common.utils.removeCompoundDrawables
import io.novafoundation.nova.common.utils.setCompoundDrawables

private class MaskingCache(val drawables: Array<out Drawable>)

fun TextView.setMaskableText(
    maskableText: MaskableModel<CharSequence?>,
    @DrawableRes maskDrawableRes: Int = R.drawable.mask_dots_small
) {
    maskableText.onHidden {
        val drawable = ContextCompat.getDrawable(context, maskDrawableRes)!!
        text = drawableText(drawable, extendToLineHeight = true)

        // Save some state to restore later
        setTag(R.id.tag_mask_cache, MaskingCache(compoundDrawables))
        removeCompoundDrawables()
    }.onUnmasked {
        text = it

        // Restore drawables state
        val maskingCache = getTag(R.id.tag_mask_cache) as? MaskingCache
        maskingCache?.let { setCompoundDrawables(maskingCache.drawables) }
    }
}
