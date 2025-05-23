package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewTitledSearchToolbarBinding
import io.novafoundation.nova.common.utils.inflater

class TitledSearchToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binder = ViewTitledSearchToolbarBinding.inflate(inflater(), this, true)

    val toolbar: Toolbar
        get() = binder.titledSearchToolbar

    val searchField: SearchView
        get() = binder.titledSearchToolbarField

    init {
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
