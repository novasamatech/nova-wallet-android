package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.R
import kotlinx.android.synthetic.main.view_titled_search_toolbar.view.titledSearchToolbar
import kotlinx.android.synthetic.main.view_titled_search_toolbar.view.titledSearchToolbarField

class TitledSearchToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val toolbar: Toolbar
        get() = titledSearchToolbar

    val searchField: SearchView
        get() = titledSearchToolbarField

    init {
        View.inflate(context, R.layout.view_titled_search_toolbar, this)

        applyAttributes(attrs)
    }

    private fun applyAttributes(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TitledSearchToolbar)

            val title = typedArray.getString(R.styleable.TitledSearchToolbar_titleText)
            toolbar.setTitle(title)

            val hint = typedArray.getString(R.styleable.TitledSearchToolbar_android_hint)
            searchField.setHint(hint)
            typedArray.recycle()
        }
    }

    fun setTitle(@StringRes titleRes: Int) {
        toolbar.setTitle(titleRes)
    }

    fun setHomeButtonListener(listener: (View) -> Unit) {
        toolbar.setHomeButtonListener(listener)
    }
}
