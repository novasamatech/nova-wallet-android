package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setShimmerShown
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.unsafeLazy
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_staking_impl.databinding.ItemDashboardNoStakeBinding
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel.StakingTypeModel

class StakingDashboardNoStakeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    private val binder = ItemDashboardNoStakeBinding.inflate(inflater(), this)

    private val imageLoader: ImageLoader

    private val earningsGroup by unsafeLazy {
        ShimmerableGroup(
            container = binder.itemDashboardNoStakeEarningsContainer,
            shimmerShape = binder.itemDashboardNoStakeEarningsShimmerShape,
            content = binder.itemDashboardNoStakeEarnings
        )
    }

    private val earningsSuffixGroup by unsafeLazy {
        ShimmerableGroup(
            container = binder.itemDashboardNoStakeEarningsSuffixContainer,
            shimmerShape = binder.itemDashboardNoStakeEarningsSuffixShimmerShape,
            content = binder.itemDashboardNoStakeEarningsSuffix
        )
    }

    init {
        background = context.getBlockDrawable().withRippleMask()

        layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(16.dp(context), 8.dp(context), 16.dp(context), 0)
        }

        imageLoader = FeatureUtils.getCommonApi(context).imageLoader()
    }

    fun setChainUi(chainUi: SyncingData<ChainUi>) {
        binder.itemDashboardNoStakeChainIcon.loadChainIcon(chainUi.data.icon, imageLoader)
        binder.itemDashboardNoStakeChainName.text = chainUi.data.name

        binder.itemDashboardNoStakeChainIcon.alpha = if (chainUi.isSyncing) 0.56f else 1.0f
        binder.itemDashboardNoStakeChainNameContainer.setShimmerShown(chainUi.isSyncing)
    }

    fun setEarnings(earningsState: ExtendedLoadingState<SyncingData<String>>) {
        earningsGroup.applyState(earningsState) { earnings ->
            binder.itemDashboardNoStakeEarnings.text = earnings
        }
        earningsSuffixGroup.applyState(earningsState)
    }

    fun setAvailableBalance(maybeBalance: String?) {
        binder.itemDashboardNoStakeChainAvailableBalance.setTextOrHide(maybeBalance)
    }

    fun setStakingTypeBadge(model: StakingTypeModel?) {
        binder.itemDashboardNoStakeStakingType.setModelOrHide(model)
    }

    fun unbind() {
        binder.itemDashboardNoStakeChainIcon.clear()
    }
}
