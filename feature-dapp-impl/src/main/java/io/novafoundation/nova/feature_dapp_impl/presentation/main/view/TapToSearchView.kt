package io.novafoundation.nova.feature_dapp_impl.presentation.main.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_dapp_impl.R

class TapToSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(ContextThemeWrapper(context, R.style.TextAppearance_NovaFoundation_Regular_SubHeadline), attrs, defStyleAttr),
    WithContextExtensions {

    override val providedContext: Context
        get() = context

    init {
        setPaddingRelative(12.dp, 0.dp, 12.dp, 0.dp)

        gravity = android.view.Gravity.CENTER_VERTICAL

        setDrawableStart(
            drawableRes = R.drawable.ic_search,
            widthInDp = 16,
            heightInDp = 16,
            paddingInDp = 6,
            tint = R.color.icon_secondary
        )

        text = context.getString(R.string.dapp_search_hint)
        setTextColorRes(R.color.hint_text)

        background = addRipple(getRoundedCornerDrawable(R.color.block_background))
    }
}
