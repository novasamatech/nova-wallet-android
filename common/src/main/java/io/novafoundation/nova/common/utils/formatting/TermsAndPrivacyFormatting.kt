package io.novafoundation.nova.common.utils.formatting

import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.text.style.UnderlineSpan
import android.widget.TextView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.clickableSpan
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.setFullSpan
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.utils.updatePadding

fun TextView.applyTermsAndPrivacyPolicy(
    containerResId: Int,
    termsResId: Int,
    privacyResId: Int,
    termsClicked: () -> Unit,
    privacyClicked: () -> Unit,
    underlineLinks: Boolean = false
) {
    updatePadding(bottom = 4.dp) // Add padding bottom to let link be underlined
    movementMethod = LinkMovementMethod.getInstance()
    highlightColor = Color.TRANSPARENT
    val linkColor = context.getColor(R.color.text_primary)

    fun link(textResId: Int, onClick: () -> Unit) = context.getString(textResId)
        .toSpannable(clickableSpan(onClick))
        .setFullSpan(colorSpan(linkColor))
        .apply { if (underlineLinks) setFullSpan(UnderlineSpan()) }

    text = SpannableFormatter.format(
        context.getString(containerResId),
        link(termsResId, termsClicked),
        link(privacyResId, privacyClicked)
    )
}
