package io.novafoundation.nova.feature_staking_impl.presentation.view.stakingType

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.google.android.material.card.MaterialCardView
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget.StakingTargetModel
import kotlinx.android.synthetic.main.view_staking_type.view.stakingTypeBackground
import kotlinx.android.synthetic.main.view_staking_type.view.stakingTypeConditions
import kotlinx.android.synthetic.main.view_staking_type.view.stakingTypeRadioButton
import kotlinx.android.synthetic.main.view_staking_type.view.stakingTypeTarget
import kotlinx.android.synthetic.main.view_staking_type.view.stakingTypeTitle

class StakingTypeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MaterialCardView(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_staking_type, this)
        setCardBackgroundColor(context.getColor(R.color.secondary_screen_background))
        strokeColor = context.getColor(R.color.staking_type_card_border)
        strokeWidth = 1.dp(context)
        radius = 12.dpF(context)
        cardElevation = 0f
    }

    fun setTitle(title: String) {
        stakingTypeTitle.text = title
    }

    fun setBackgroundRes(@DrawableRes resId: Int) {
        val drawable = ContextCompat.getDrawable(context, resId) ?: return
        stakingTypeBackground.setImageDrawable(drawable)
        stakingTypeBackground.updateLayoutParams<FrameLayout.LayoutParams> {
            this.height = drawable.intrinsicHeight
        }
    }

    fun select(isSelected: Boolean) {
        stakingTypeRadioButton.isChecked = isSelected
        strokeColor = if (isSelected) {
            context.getColor(R.color.active_border)
        } else {
            context.getColor(R.color.staking_type_card_border)
        }
    }

    fun setConditions(conditions: List<String>) {
        stakingTypeConditions.text = conditions.joinToString(separator = "\n") { it }
    }

    fun setSelectable(isSelectable: Boolean) {
        if (isSelectable) {
            stakingTypeTitle.setTextColorRes(R.color.text_primary)
            stakingTypeConditions.setTextColorRes(R.color.text_secondary)
        } else {
            stakingTypeTitle.setTextColorRes(R.color.button_text_inactive)
            stakingTypeConditions.setTextColorRes(R.color.button_text_inactive)
        }
    }

    fun setStakingTarget(stakingTarget: StakingTargetModel?) {
        if (stakingTarget == null) {
            stakingTypeTarget.makeGone()
            return
        }

        stakingTypeTarget.makeVisible()
        stakingTypeTarget.setModel(stakingTarget)
    }
}
