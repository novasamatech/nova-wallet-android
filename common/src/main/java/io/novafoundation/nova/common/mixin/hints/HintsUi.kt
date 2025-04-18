package io.novafoundation.nova.common.mixin.hints

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.utils.useAttributes

class HintsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    init {
        orientation = VERTICAL

        attrs?.let(::applyAttributes)
    }

    fun setHints(hints: List<CharSequence>) {
        removeAllViews()

        hints.mapIndexed { index, hint ->
            TextView(context).apply {
                setTextAppearance(R.style.TextAppearance_NovaFoundation_Regular_Caption1)

                setTextColorRes(R.color.text_secondary)
                setDrawableStart(R.drawable.ic_nova, widthInDp = 16, paddingInDp = 8, tint = R.color.icon_secondary)

                text = hint

                if (index > 0) {
                    updatePadding(top = 12.dp)
                }
            }
        }.forEach(::addView)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.HintsView) {
        val singleHint = it.getText(R.styleable.HintsView_HintsView_singleHint)
        singleHint?.let(::setSingleHint)
    }
}

fun HintsView.setSingleHint(hint: CharSequence) {
    setHints(listOf(hint))
}

fun BaseFragment<*, *>.observeHints(mixin: HintsMixin, view: HintsView) {
    mixin.hintsFlow.observe(view::setHints)
}
