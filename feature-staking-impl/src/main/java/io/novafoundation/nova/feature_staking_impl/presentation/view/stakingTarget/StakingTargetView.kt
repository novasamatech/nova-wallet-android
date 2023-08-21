package io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeGoneViews
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.makeVisibleViews
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.view_staking_target.view.stakingTargetIcon
import kotlinx.android.synthetic.main.view_staking_target.view.stakingTargetIconShimmer
import kotlinx.android.synthetic.main.view_staking_target.view.stakingTargetQuantity
import kotlinx.android.synthetic.main.view_staking_target.view.stakingTargetSubtitle
import kotlinx.android.synthetic.main.view_staking_target.view.stakingTargetSubtitleShimmering
import kotlinx.android.synthetic.main.view_staking_target.view.stakingTargetTitle
import kotlinx.android.synthetic.main.view_staking_target.view.stakingTargetTitleShimmering

class StakingTargetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        View.inflate(context, R.layout.view_staking_target, this)

        background = context.getRoundedCornerDrawable(fillColorRes = R.color.block_background, cornerSizeInDp = 8)
        stakingTargetQuantity.background = context.getRoundedCornerDrawable(fillColorRes = R.color.chips_background, cornerSizeInDp = 6)
    }

    fun setLoadingState() {
        makeVisibleViews(stakingTargetTitleShimmering, stakingTargetSubtitleShimmering, stakingTargetIconShimmer)
        makeGoneViews(stakingTargetTitle, stakingTargetSubtitle, stakingTargetQuantity, stakingTargetIcon)
    }

    fun setModel(stakingTargetModel: StakingTargetModel) {
        stakingTargetTitle.text = stakingTargetModel.title
        stakingTargetSubtitle.setTextOrHide(stakingTargetModel.subtitle)
        stakingTargetSubtitle.setTextColorRes(stakingTargetModel.subtitleColorRes)

        makeGoneViews(stakingTargetTitleShimmering, stakingTargetSubtitleShimmering, stakingTargetIconShimmer)

        when (val icon = stakingTargetModel.icon) {
            is StakingTargetModel.TargetIcon.Icon -> {
                stakingTargetQuantity.makeGone()
                stakingTargetIcon.makeVisible()
                stakingTargetIcon.setIcon(icon.icon, imageLoader)
            }
            is StakingTargetModel.TargetIcon.Quantity -> {
                stakingTargetIcon.makeGone()
                stakingTargetQuantity.makeVisible()
                stakingTargetQuantity.text = icon.quantity
            }
            null -> {
                makeGoneViews(stakingTargetIcon, stakingTargetQuantity)
                stakingTargetIcon.makeGone()
                stakingTargetQuantity.makeGone()
            }
        }
    }
}
