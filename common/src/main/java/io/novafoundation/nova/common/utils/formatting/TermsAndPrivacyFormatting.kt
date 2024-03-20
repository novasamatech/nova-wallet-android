package io.novafoundation.nova.common.utils.formatting

import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.widget.TextView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.clickableSpan
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.setFullSpan
import io.novafoundation.nova.common.utils.toSpannable

fun TextView.applyTermsAndPrivacyPolicy(
    containerResId: Int,
    termsResId: Int,
    privacyResId: Int,
    termsClicked: () -> Unit,
    privacyClicked: () -> Unit
) {
    movementMethod = LinkMovementMethod.getInstance()
    highlightColor = Color.TRANSPARENT
    val linkColor = context.getColor(R.color.text_primary)

    text = SpannableFormatter.format(
        context.getString(containerResId),
        context.getString(termsResId)
            .toSpannable(clickableSpan(termsClicked))
            .setFullSpan(colorSpan(linkColor)),
        context.getString(privacyResId)
            .toSpannable(clickableSpan(privacyClicked))
            .setFullSpan(colorSpan(linkColor))
    )
}
