package io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.view_staking_target.view.stakingTargetChevron
import kotlinx.android.synthetic.main.view_staking_target.view.stakingTargetIcon
import kotlinx.android.synthetic.main.view_staking_target.view.stakingTargetIconShimmerContainer
import kotlinx.android.synthetic.main.view_staking_target.view.stakingTargetQuantity
import kotlinx.android.synthetic.main.view_staking_target.view.stakingTargetSubtitle
import kotlinx.android.synthetic.main.view_staking_target.view.stakingTargetSubtitleShimmering
import kotlinx.android.synthetic.main.view_staking_target.view.stakingTargetTitle


class StakingTargetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_staking_target, this)

        background = context.getRoundedCornerDrawable(fillColorRes = R.color.block_background, cornerSizeInDp = 8)
        stakingTargetQuantity.background = context.getRoundedCornerDrawable(fillColorRes = R.color.chips_background, cornerSizeInDp = 6)
    }

    fun setLoadingState() {
        stakingTargetSubtitleShimmering.startShimmer()
        stakingTargetSubtitleShimmering.startShimmer()
        stakingTargetIconShimmerContainer.startShimmer()
    }

    fun setModel(stakingTargetModel: StakingTargetModel) {
        stakingTargetTitle.text = stakingTargetModel.title
        stakingTargetSubtitle.setTextOrHide(stakingTargetModel.subtitle)

        stakingTargetSubtitleShimmering.stopShimmer()
        stakingTargetSubtitleShimmering.stopShimmer()
        stakingTargetIconShimmerContainer.stopShimmer()

        when (stakingTargetModel.icon) {
            is StakingTargetModel.Icon.Drawable -> {
                stakingTargetIconShimmerContainer.makeVisible()
                stakingTargetQuantity.makeGone()
                stakingTargetIcon.makeVisible()
                stakingTargetIcon.setImageDrawable(stakingTargetModel.icon.drawable)
            }
            is StakingTargetModel.Icon.Quantity -> {
                stakingTargetIconShimmerContainer.makeVisible()
                stakingTargetChevron.makeGone()
                stakingTargetQuantity.makeVisible()
                stakingTargetQuantity.text = stakingTargetModel.icon.quantity
            }
            null -> stakingTargetIconShimmerContainer.makeGone()
        }
    }
}
