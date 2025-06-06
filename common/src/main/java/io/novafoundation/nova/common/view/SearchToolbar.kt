package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewSearchToolbarBinding
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.useAttributes

class SearchToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binder = ViewSearchToolbarBinding.inflate(inflater(), this)

    val searchInput
        get() = binder.searchToolbarSearch

    val cancel
        get() = binder.searchToolbarCancel

    init {
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
