package io.novafoundation.nova.common.view.tabs

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import androidx.annotation.StringRes
import androidx.core.view.children
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable

typealias OnTabSelected = (index: Int) -> Unit

private val DEFAULT_BACKGROUND_TINT = R.color.android_app_nav_bar_background

class TabsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    private var activeTab: Int? = null
    private var onTabSelected: OnTabSelected? = null

    init {
        updatePadding(top = 4.dp, bottom = 4.dp, start = 4.dp)

        attrs?.let(::applyAttributes)
    }

    fun addTab(title: String) {
        val tab = TabItem(context).apply {
            text = title
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                weight = 1.0f
                marginEnd = 4.dp
            }

            setOnClickListener { clickedView ->
                setCheckedTab(indexOfChild(clickedView), triggerListener = true)
            }
        }

        addView(tab)
    }

    fun setCheckedTab(newActiveTab: Int, triggerListener: Boolean) {
        val previousTab = activeTab

        activeTab = newActiveTab

        if (previousTab != newActiveTab && triggerListener) {
            onTabSelected?.invoke(newActiveTab)
        }

        // we need to update checked states even if the same button was clicked since
        // CompoundButton (which is parent for TabItem) toggles state internally on every click
        children.filterIsInstance<TabItem>()
            .forEachIndexed { index, tabItem ->
                tabItem.isChecked = index == activeTab
            }
    }

    fun onTabSelected(listener: OnTabSelected) {
        onTabSelected = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = MeasureSpec.makeMeasureSpec(40.dp, MeasureSpec.EXACTLY)

        super.onMeasure(widthMeasureSpec, height)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.TabsView) {
        val backgroundTint = it.getResourceId(R.styleable.TabsView_TabsView_backgroundTint, DEFAULT_BACKGROUND_TINT)
        background = context.getRoundedCornerDrawable(fillColorRes = backgroundTint)
    }
}

fun TabsView.addTab(@StringRes titleRes: Int) {
    addTab(context.getString(titleRes))
}
