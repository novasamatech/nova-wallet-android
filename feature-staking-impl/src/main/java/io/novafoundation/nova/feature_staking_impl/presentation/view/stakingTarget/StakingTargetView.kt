package io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.setColoredTextOrHide
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.getRippleMask
import io.novafoundation.nova.common.utils.getRoundedCornerDrawable
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeGoneViews
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.makeVisibleViews
import io.novafoundation.nova.common.utils.withRippleMask
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ViewStakingTargetBinding

class StakingTargetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    private val binder = ViewStakingTargetBinding.inflate(inflater(), this)

    init {
        minHeight = 52.dp(context)

        background = getRoundedCornerDrawable(fillColorRes = R.color.block_background, cornerSizeDp = 8)
            .withRippleMask(getRippleMask(cornerSizeDp = 8))

        binder.stakingTargetQuantity.background = context.getRoundedCornerDrawable(fillColorRes = R.color.chips_background, cornerSizeInDp = 6)
    }

    fun setLoadingState() {
        makeVisibleViews(binder.stakingTargetTitleShimmering, binder.stakingTargetSubtitleShimmering, binder.stakingTargetIconShimmer)
        makeGoneViews(binder.stakingTargetTitle, binder.stakingTargetSubtitle, binder.stakingTargetQuantity, binder.stakingTargetIcon)
    }

    fun setModel(stakingTargetModel: StakingTargetModel) {
        binder.stakingTargetTitle.text = stakingTargetModel.title
        binder.stakingTargetTitle.makeVisible()

        binder.stakingTargetSubtitle.setColoredTextOrHide(stakingTargetModel.subtitle)

        makeGoneViews(binder.stakingTargetTitleShimmering, binder.stakingTargetSubtitleShimmering, binder.stakingTargetIconShimmer)

        when (val icon = stakingTargetModel.icon) {
            is StakingTargetModel.TargetIcon.Icon -> {
                binder.stakingTargetQuantity.makeGone()
                binder.stakingTargetIcon.makeVisible()
                binder.stakingTargetIcon.setIcon(icon.icon, imageLoader)
            }

            is StakingTargetModel.TargetIcon.Quantity -> {
                binder.stakingTargetIcon.makeGone()
                binder.stakingTargetQuantity.makeVisible()
                binder.stakingTargetQuantity.text = icon.quantity
            }

            null -> {
                makeGoneViews(binder.stakingTargetIcon, binder.stakingTargetQuantity)
                binder.stakingTargetIcon.makeGone()
                binder.stakingTargetQuantity.makeGone()
            }
        }
    }
}
