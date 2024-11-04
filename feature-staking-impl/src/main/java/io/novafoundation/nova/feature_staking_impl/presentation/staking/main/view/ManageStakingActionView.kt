package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ItemStakingManageActionBinding

class ManageStakingActionView @kotlin.jvm.JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private val binder = ItemStakingManageActionBinding.inflate(inflater(), this)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        setBackgroundResource(R.drawable.bg_primary_list_item)

        attrs?.let(::applyAttrs)
    }

    fun setLabel(label: String) {
        binder.itemManageStakingActionText.text = label
    }

    fun setIcon(icon: Drawable) {
        binder.itemManageStakingActionImage.setImageDrawable(icon)
    }

    fun setIconRes(@DrawableRes iconRes: Int) {
        binder.itemManageStakingActionImage.setImageResource(iconRes)
    }

    fun setBadge(content: String?) {
        binder.itemManageStakingActionBadge.setTextOrHide(content)
    }

    private fun applyAttrs(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.ManageStakingActionView) {
        val label = it.getString(R.styleable.ManageStakingActionView_label)
        label?.let(::setLabel)

        val icon = it.getDrawable(R.styleable.ManageStakingActionView_icon)
        icon?.let(::setIcon)
    }
}
