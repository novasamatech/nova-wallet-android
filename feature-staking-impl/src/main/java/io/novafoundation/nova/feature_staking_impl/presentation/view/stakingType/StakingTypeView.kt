package io.novafoundation.nova.feature_staking_impl.presentation.view.stakingType

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.google.android.material.card.MaterialCardView
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ViewStakingTypeBinding
import io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget.StakingTargetModel

class StakingTypeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val binder = ViewStakingTypeBinding.inflate(inflater(), this)

    init {
        setCardBackgroundColor(context.getColor(R.color.secondary_screen_background))
        strokeColor = context.getColor(R.color.staking_type_card_border)
        strokeWidth = 1.dp(context)
        radius = 12.dpF(context)
        cardElevation = 0f
    }

    fun setTitle(title: String) {
        binder.stakingTypeTitle.text = title
    }

    fun setBackgroundRes(@DrawableRes resId: Int) {
        val drawable = ContextCompat.getDrawable(context, resId) ?: return
        binder.stakingTypeBackground.setImageDrawable(drawable)
        binder.stakingTypeBackground.updateLayoutParams<FrameLayout.LayoutParams> {
            this.height = drawable.intrinsicHeight
        }
    }

    fun select(isSelected: Boolean) {
        binder.stakingTypeRadioButton.isChecked = isSelected
        strokeColor = if (isSelected) {
            context.getColor(R.color.active_border)
        } else {
            context.getColor(R.color.staking_type_card_border)
        }
    }

    fun setConditions(conditions: List<String>) {
        binder.stakingTypeConditions.text = conditions.joinToString(separator = "\n") { it }
    }

    fun setSelectable(isSelectable: Boolean) {
        if (isSelectable) {
            binder.stakingTypeTitle.setTextColorRes(R.color.text_primary)
            binder.stakingTypeConditions.setTextColorRes(R.color.staking_type_banner_text)
        } else {
            binder.stakingTypeTitle.setTextColorRes(R.color.staking_type_banner_text_inactive)
            binder.stakingTypeConditions.setTextColorRes(R.color.staking_type_banner_text_inactive)
        }
    }

    fun setStakingTarget(stakingTarget: StakingTargetModel?) {
        if (stakingTarget == null) {
            binder.stakingTypeTarget.makeGone()
            return
        }

        binder.stakingTypeTarget.makeVisible()
        binder.stakingTypeTarget.setModel(stakingTarget)
    }

    fun setStakingTargetClickListener(listener: OnClickListener) {
        binder.stakingTypeTarget.setOnClickListener(listener)
    }
}
