package io.novafoundation.nova.common.mixin.hints

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.updatePadding

class HintsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    init {
        orientation = VERTICAL
    }

    fun setHints(hints: List<String>) {
        removeAllViews()

        hints.map { hint ->
            TextView(context).apply {
                setTextAppearance(R.style.TextAppearance_NovaFoundation_Regular_Caption1)

                setDrawableStart(R.drawable.ic_nova, widthInDp = 16, paddingInDp = 8, tint = R.color.white_48)
                text = hint

                updatePadding(top = 8.dp)
            }
        }.forEach(::addView)
    }
}

fun BaseFragment<*>.observeHints(mixin: HintsMixin, view: HintsView) {
    mixin.hints.observe { view.setHints(it) }
}
