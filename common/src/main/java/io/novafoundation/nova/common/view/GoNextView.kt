package io.novafoundation.nova.common.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import kotlinx.android.synthetic.main.view_go_next.view.goNextActionImage
import kotlinx.android.synthetic.main.view_go_next.view.goNextBadgeText
import kotlinx.android.synthetic.main.view_go_next.view.goNextDivider
import kotlinx.android.synthetic.main.view_go_next.view.goNextIcon
import kotlinx.android.synthetic.main.view_go_next.view.goNextProgress
import kotlinx.android.synthetic.main.view_go_next.view.goNextTitle

class GoNextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_go_next, this)

        attrs?.let(this::applyAttributes)
    }

    val icon: ImageView
        get() = goNextIcon

    val title: TextView
        get() = goNextTitle

    fun setInProgress(inProgress: Boolean) {
        isEnabled = !inProgress

        goNextActionImage.setVisible(!inProgress)
        goNextProgress.setVisible(inProgress)
    }

    fun setDividerVisible(visible: Boolean) {
        goNextDivider.setVisible(visible)
    }

    fun setBadgeText(badgeText: String?) {
        goNextBadgeText.setTextOrHide(badgeText)
    }

    fun loadIcon(iconLink: String, imageLoader: ImageLoader) {
        icon.load(iconLink, imageLoader)
        icon.setVisible(true)
    }

    fun setProgressTint(@ColorRes tintColor: Int) {
        goNextProgress.indeterminateTintList = ColorStateList.valueOf(context.getColor(tintColor))
    }

    fun setIcon(drawable: Drawable?) {
        icon.setImageDrawable(drawable)
        icon.setVisible(drawable != null)
    }

    fun setIconTint(colorStateList: ColorStateList?) {
        icon.imageTintList = colorStateList
    }

    fun setActionTint(@ColorInt color: Int) {
        goNextActionImage.imageTintList = ColorStateList.valueOf(color)
        goNextBadgeText.setTextColor(color)
    }

    private fun applyAttributes(attributeSet: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.GoNextView)

        val titleDisplay = typedArray.getString(R.styleable.GoNextView_android_text)
        title.text = titleDisplay

        val inProgress = typedArray.getBoolean(R.styleable.GoNextView_inProgress, false)
        setInProgress(inProgress)

        val iconDrawable = typedArray.getDrawable(R.styleable.GoNextView_icon)
        setIcon(iconDrawable)

        val iconTint = typedArray.getColorStateList(R.styleable.GoNextView_iconTint)
        setIconTint(iconTint)

        val actionIconDrawable = typedArray.getDrawable(R.styleable.GoNextView_actionIcon)
        goNextActionImage.setImageDrawable(actionIconDrawable)

        val dividerVisible = typedArray.getBoolean(R.styleable.GoNextView_dividerVisible, true)
        setDividerVisible(dividerVisible)

        val backgroundDrawable = typedArray.getDrawable(R.styleable.GoNextView_android_background)
        if (backgroundDrawable != null) background = backgroundDrawable else setBackgroundResource(R.drawable.bg_primary_list_item)

        val textAppearance = typedArray.getResourceIdOrNull(R.styleable.GoNextView_android_textAppearance)
        textAppearance?.let(title::setTextAppearance)

        val actionTint = typedArray.getColor(R.styleable.GoNextView_actionTint, context.getColor(R.color.icon_primary))
        setActionTint(actionTint)

        typedArray.recycle()
    }
}
