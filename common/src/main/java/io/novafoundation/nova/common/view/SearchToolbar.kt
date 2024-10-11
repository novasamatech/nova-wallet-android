package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.useAttributes

class SearchToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    val searchInput
        get() = searchToolbarSearch

    val cancel
        get() = searchToolbarCancel

    init {
        View.inflate(context, R.layout.view_search_toolbar, this)

        orientation = HORIZONTAL
        setBackgroundResource(R.color.blur_navigation_background)

        attrs?.let(::applyAttributes)
    }

    fun setHint(hint: String) {
        searchInput.setHint(hint)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.SearchToolbar) { typedArray ->
        val hint = typedArray.getString(R.styleable.SearchToolbar_android_hint)
        hint?.let(::setHint)
    }
}
