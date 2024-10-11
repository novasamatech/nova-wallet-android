package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setVisible

class Toolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val rightActionText: TextView
        get() = rightText

    val titleView: TextView
        get() = titleTv

    init {
        View.inflate(context, R.layout.view_toolbar, this)

        applyAttributes(attrs)
    }

    private fun applyAttributes(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.Toolbar)

            val title = typedArray.getString(R.styleable.Toolbar_titleText)
            setTitle(title)

            val rightIcon = typedArray.getDrawable(R.styleable.Toolbar_iconRight)
            rightIcon?.let { setRightIconDrawable(it) }

            val action = typedArray.getString(R.styleable.Toolbar_textRight)
            action?.let { setTextRight(it) }

            val homeButtonIcon = typedArray.getDrawable(R.styleable.Toolbar_homeButtonIcon)
            homeButtonIcon?.let { setHomeButtonIcon(it) }

            val homeButtonVisible = typedArray.getBoolean(R.styleable.Toolbar_homeButtonVisible, true)
            setHomeButtonVisibility(homeButtonVisible)

            val dividerVisible = typedArray.getBoolean(R.styleable.Toolbar_dividerVisible, true)
            toolbarDivider.setVisible(dividerVisible)

            val backgroundAttrDrawable = typedArray.getDrawable(R.styleable.Toolbar_contentBackground) ?: ColorDrawable(
                context.getColor(R.color.secondary_screen_background)
            )
            toolbarContainer.background = backgroundAttrDrawable

            val textAppearance = typedArray.getResourceIdOrNull(R.styleable.Toolbar_titleTextAppearance)
            textAppearance?.let(titleTv::setTextAppearance)

            typedArray.recycle()
        }
    }

    fun setHomeButtonIcon(icon: Drawable) {
        backImg.setImageDrawable(icon)
    }

    fun setTextRight(action: String) {
        rightImg.makeGone()

        rightText.makeVisible()
        rightText.text = action
    }

    fun setRightIconVisible(visible: Boolean) {
        rightImg.setVisible(visible)
    }

    fun showProgress(visible: Boolean) {
        toolbarProgress.setVisible(visible)
        rightActionContainer.setVisible(!visible)
    }

    fun setTitleIcon(drawable: Drawable?) {
        titleTv.compoundDrawablePadding = 8.dp(context)
        titleTv.setCompoundDrawables(drawable, null, null, null)
    }

    fun setTitle(title: CharSequence?) {
        titleTv.text = title
    }

    fun setTitle(@StringRes titleRes: Int) {
        titleTv.setText(titleRes)
    }

    fun showHomeButton() {
        backImg.makeVisible()
    }

    fun hideHomeButton() {
        backImg.makeGone()
    }

    fun setHomeButtonListener(listener: (View) -> Unit) {
        backImg.setOnClickListener(listener)
    }

    fun hideRightAction() {
        rightImg.makeGone()
        rightText.makeGone()
    }

    fun setRightActionTint(@ColorRes colorRes: Int) {
        rightImg.setImageTintRes(colorRes)
        rightText.setTextColorRes(colorRes)
    }

    fun setRightIconRes(@DrawableRes iconRes: Int) {
        val drawable = ContextCompat.getDrawable(context, iconRes)
        drawable?.let { setRightIconDrawable(it) }
    }

    fun setRightIconDrawable(assetIconDrawable: Drawable) {
        rightText.makeGone()

        rightImg.makeVisible()
        rightImg.setImageDrawable(assetIconDrawable)
    }

    fun setRightActionClickListener(listener: (View) -> Unit) {
        rightImg.setOnClickListener(listener)
        rightText.setOnClickListener(listener)
    }

    fun setHomeButtonVisibility(visible: Boolean) {
        backImg.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun addCustomAction(@DrawableRes icon: Int, onClick: OnClickListener): ImageView {
        val actionView = ImageView(context).apply {
            setImageResource(icon)

            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                val verticalMargin = 16.dp(context)

                val endMarginDp = if (this@Toolbar.toolbarCustomActions.childCount == 0) 16 else 10
                val endMargin = endMarginDp.dp(context)

                val startMargin = 10.dp(context)

                setMargins(startMargin, verticalMargin, endMargin, verticalMargin)
            }

            setOnClickListener(onClick)
        }

        toolbarCustomActions.makeVisible()
        toolbarCustomActions.addView(actionView, 0)

        return actionView
    }

    fun setRightActionEnabled(enabled: Boolean) {
        rightImg.isEnabled = enabled
        rightText.isEnabled = enabled
    }
}
