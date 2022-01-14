package io.novafoundation.nova.feature_dapp_impl.presentation.search

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.onTextChanged
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_dapp_impl.R
import kotlinx.android.synthetic.main.view_search.view.searchClear
import kotlinx.android.synthetic.main.view_search.view.searchContent

class SearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions {

    override val providedContext: Context
        get() = context

    val content: EditText
        get() = searchContent

    init {
        View.inflate(context, R.layout.view_search, this)

        background = getRoundedCornerDrawable(fillColorRes = R.color.white_8)

        orientation = HORIZONTAL

        content.onTextChanged {
            searchClear.setVisible(it.isNotEmpty())
        }
        searchClear.setOnClickListener {
            content.text.clear()
        }
    }
}
