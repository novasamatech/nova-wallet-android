package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_staking_impl.R

class ManageStakingActionView @kotlin.jvm.JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.item_staking_manage_action, this)
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        setBackgroundResource(R.drawable.bg_primary_list_item)

        attrs?.let(::applyAttrs)
    }

    fun setLabel(label: String) {
        itemManageStakingActionText.text = label
    }

    fun setIcon(icon: Drawable) {
        itemManageStakingActionImage.setImageDrawable(icon)
    }

    fun setIconRes(@DrawableRes iconRes: Int) {
        itemManageStakingActionImage.setImageResource(iconRes)
    }

    fun setBadge(content: String?) {
        itemManageStakingActionBadge.setTextOrHide(content)
    }

    private fun applyAttrs(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.ManageStakingActionView) {
        val label = it.getString(R.styleable.ManageStakingActionView_label)
        label?.let(::setLabel)

        val icon = it.getDrawable(R.styleable.ManageStakingActionView_icon)
        icon?.let(::setIcon)
    }
}
