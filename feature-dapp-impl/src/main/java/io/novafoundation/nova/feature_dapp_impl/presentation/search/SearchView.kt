package io.novafoundation.nova.feature_dapp_impl.presentation.search

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_dapp_impl.R

class SearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatEditText(context, attrs, defStyleAttr) {

    init {
        background = context.getRoundedCornerDrawable(fillColorRes = R.color.white_8)

        setDrawableEnd(R.drawable.ic_close_circle, paddingInDp = 16)

        setPaddingRelative(12.dp(context), 9.dp(context), 8.dp(context), 12.dp(context))

        imeOptions = EditorInfo.IME_ACTION_SEARCH
        includeFontPadding = false
        isSingleLine = true

        setHint(R.string.dapp_search_hint)
        setHintTextColor(context.getColor(R.color.white_32))
    }
}
