package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import android.widget.LinearLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewSearchBinding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.onTextChanged
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.useAttributes

class SearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions {

    private val binder = ViewSearchBinding.inflate(inflater(), this)

    override val providedContext: Context
        get() = context

    val content: EditText
        get() = binder.searchContent

    init {
        background = getRoundedCornerDrawable(fillColorRes = R.color.input_background, cornerSizeDp = 10)

        orientation = HORIZONTAL

        content.onTextChanged {
            binder.searchClear.setVisible(it.isNotEmpty())
        }
        binder.searchClear.setOnClickListener {
            content.text.clear()
        }

        attrs?.let(::applyAttrs)
    }

    fun setHint(hint: String?) {
        content.hint = hint
    }

    private fun applyAttrs(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.SearchView) {
        val hint = it.getString(R.styleable.SearchView_android_hint)
        setHint(hint)
    }
}
