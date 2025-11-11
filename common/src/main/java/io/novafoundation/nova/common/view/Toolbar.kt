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
import io.novafoundation.nova.common.databinding.ViewToolbarBinding
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.inflater
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

    private val binder = ViewToolbarBinding.inflate(inflater(), this, true)

    val rightActionText: TextView
        get() = binder.rightText

    val titleView: TextView
        get() = binder.titleTv

    init {
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

            val textRightVisible = typedArray.getBoolean(R.styleable.Toolbar_textRightVisible, false)
            setRightTextVisible(textRightVisible)

            val homeButtonIcon = typedArray.getDrawable(R.styleable.Toolbar_homeButtonIcon)
            homeButtonIcon?.let { setHomeButtonIcon(it) }

            val homeButtonVisible = typedArray.getBoolean(R.styleable.Toolbar_homeButtonVisible, true)
            setHomeButtonVisibility(homeButtonVisible)

            val dividerVisible = typedArray.getBoolean(R.styleable.Toolbar_dividerVisible, true)
            binder.toolbarDivider.setVisible(dividerVisible)

            val backgroundAttrDrawable = typedArray.getDrawable(R.styleable.Toolbar_contentBackground) ?: ColorDrawable(
                context.getColor(R.color.secondary_screen_background)
            )
            binder.toolbarContainer.background = backgroundAttrDrawable

            val textAppearance = typedArray.getResourceIdOrNull(R.styleable.Toolbar_titleTextAppearance)
            textAppearance?.let(binder.titleTv::setTextAppearance)

            typedArray.recycle()
        }
    }

    fun setHomeButtonIcon(@DrawableRes iconRes: Int) {
        binder.backImg.setImageResource(iconRes)
    }

    fun setHomeButtonIcon(icon: Drawable) {
        binder.backImg.setImageDrawable(icon)
    }

    fun setTextRight(action: String) {
        binder.rightImg.makeGone()

        binder.rightText.makeVisible()
        binder.rightText.text = action
    }

    fun setRightIconVisible(visible: Boolean) {
        binder.rightImg.setVisible(visible)
    }

    fun setRightTextVisible(visible: Boolean) {
        binder.rightText.setVisible(visible)
    }

    fun showProgress(visible: Boolean) {
        binder.toolbarProgress.setVisible(visible)
        binder.rightActionContainer.setVisible(!visible)
    }

    fun setTitleIcon(drawable: Drawable?) {
        binder.titleTv.compoundDrawablePadding = 8.dp(context)
        binder.titleTv.setCompoundDrawables(drawable, null, null, null)
    }

    fun setTitle(title: CharSequence?) {
        binder.titleTv.text = title
    }

    fun setTitle(@StringRes titleRes: Int) {
        binder.titleTv.setText(titleRes)
    }

    fun showHomeButton() {
        binder.backImg.makeVisible()
    }

    fun hideHomeButton() {
        binder.backImg.makeGone()
    }

    fun setHomeButtonListener(listener: (View) -> Unit) {
        binder.backImg.setOnClickListener(listener)
    }

    fun hideRightAction() {
        binder.rightImg.makeGone()
        binder.rightText.makeGone()
    }

    fun setRightActionTint(@ColorRes colorRes: Int) {
        binder.rightImg.setImageTintRes(colorRes)
        binder.rightText.setTextColorRes(colorRes)
    }

    fun setRightIconRes(@DrawableRes iconRes: Int) {
        val drawable = ContextCompat.getDrawable(context, iconRes)
        drawable?.let { setRightIconDrawable(it) }
    }

    fun setRightIconDrawable(assetIconDrawable: Drawable) {
        binder.rightText.makeGone()

        binder.rightImg.makeVisible()
        binder.rightImg.setImageDrawable(assetIconDrawable)
    }

    fun setRightActionClickListener(listener: (View) -> Unit) {
        binder.rightImg.setOnClickListener(listener)
        binder.rightText.setOnClickListener(listener)
    }

    fun setHomeButtonVisibility(visible: Boolean) {
        binder.backImg.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun addCustomAction(@DrawableRes icon: Int, onClick: OnClickListener): ImageView {
        val actionView = ImageView(context).apply {
            setImageResource(icon)

            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                val verticalMargin = 16.dp(context)

                val endMarginDp = if (binder.toolbarCustomActions.childCount == 0) 16 else 10
                val endMargin = endMarginDp.dp(context)

                val startMargin = 10.dp(context)

                setMargins(startMargin, verticalMargin, endMargin, verticalMargin)
            }

            setOnClickListener(onClick)
        }

        binder.toolbarCustomActions.makeVisible()
        binder.toolbarCustomActions.addView(actionView, 0)

        return actionView
    }

    fun setRightActionEnabled(enabled: Boolean) {
        binder.rightImg.isEnabled = enabled
        binder.rightText.isEnabled = enabled
    }
}
