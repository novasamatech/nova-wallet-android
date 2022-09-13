package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.useAttributes
import kotlinx.android.synthetic.main.view_link.view.viewLinkText

class LinkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {
    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        View.inflate(context, R.layout.view_link, this)

        attrs?.let(::applyAttributes)
    }

    private fun applyAttributes(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.LinkView) {
        val linkText = it.getString(R.styleable.LinkView_linkText)
        viewLinkText.text = linkText
    }
}
