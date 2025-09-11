package io.novafoundation.nova.common.presentation.masking

import android.widget.TextView
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.setForegroundRes

fun <T, R> MaskableModel<T>.map(mapper: (T) -> R): MaskableModel<R> = when (this) {
    is MaskableModel.Hidden -> MaskableModel.Hidden()
    is MaskableModel.Unmasked -> MaskableModel.Unmasked(mapper(value))
}

fun TextView.setMaskableText(maskableText: MaskableModel<CharSequence?>, @DrawableRes maskDrawableRes: Int = R.drawable.mask_dots_small) {
    maskableText.onHidden {
        text = null
        setForegroundRes(maskDrawableRes)
    }.onUnmasked {
        text = it
    }
}

private fun <T> MaskableModel<T>.onHidden(onHidden: () -> Unit): MaskableModel<T> {
    if (this is MaskableModel.Hidden) {
        onHidden()
    }
    return this
}

private fun <T> MaskableModel<T>.onUnmasked(onUnmasked: (T) -> Unit): MaskableModel<T> {
    if (this is MaskableModel.Unmasked) {
        onUnmasked(this.value)
    }
    return this
}
