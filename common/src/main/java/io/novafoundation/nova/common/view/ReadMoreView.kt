package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.updatePadding


class ReadMoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(ContextThemeWrapper(context, R.style.TextAppearance_NovaFoundation_Regular_SubHeadline), attrs, defStyleAttr),
    WithContextExtensions by WithContextExtensions(context) {

    init {
        gravity = Gravity.CENTER_VERTICAL
        updatePadding(top = 7.dp, bottom = 7.dp)
        setText(R.string.common_read_more)
        setTextColorRes(R.color.button_text_accent)
        setDrawableEnd(R.drawable.ic_chevron_right, widthInDp = 16, tint = R.color.icon_accent)
        includeFontPadding = false
    }
}
