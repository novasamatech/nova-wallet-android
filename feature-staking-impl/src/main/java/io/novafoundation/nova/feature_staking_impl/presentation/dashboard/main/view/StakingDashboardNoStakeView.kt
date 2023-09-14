package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.setShimmerShown
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.unsafeLazy
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel.StakingTypeModel
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeChainAvailableBalance
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeChainIcon
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeChainName
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeChainNameContainer
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeEarnings
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeEarningsContainer
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeEarningsShimmerShape
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeEarningsSuffix
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeEarningsSuffixContainer
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeEarningsSuffixShimmerShape
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeStakingType

class StakingDashboardNoStakeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    private val imageLoader: ImageLoader

    private val earningsGroup by unsafeLazy {
        ShimmerableGroup(
            container = itemDashboardNoStakeEarningsContainer,
            shimmerShape = itemDashboardNoStakeEarningsShimmerShape,
            content = itemDashboardNoStakeEarnings
        )
    }

    private val earningsSuffixGroup by unsafeLazy {
        ShimmerableGroup(
            container = itemDashboardNoStakeEarningsSuffixContainer,
            shimmerShape = itemDashboardNoStakeEarningsSuffixShimmerShape,
            content = itemDashboardNoStakeEarningsSuffix
        )
    }

    init {
        View.inflate(context, R.layout.item_dashboard_no_stake, this)

        background = context.getBlockDrawable().withRippleMask()

        layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(16.dp(context), 8.dp(context), 16.dp(context), 0)
        }

        imageLoader = FeatureUtils.getCommonApi(context).imageLoader()
    }

    fun setChainUi(chainUi: SyncingData<ChainUi>) {
        itemDashboardNoStakeChainIcon.loadChainIcon(chainUi.data.icon, imageLoader)
        itemDashboardNoStakeChainName.text = chainUi.data.name

        itemDashboardNoStakeChainIcon.alpha = if (chainUi.isSyncing) 0.56f else 1.0f
        itemDashboardNoStakeChainNameContainer.setShimmerShown(chainUi.isSyncing)
    }

    fun setEarnings(earningsState: ExtendedLoadingState<SyncingData<String>>) {
        earningsGroup.applyState(earningsState) { earnings ->
            itemDashboardNoStakeEarnings.text = earnings
        }
        earningsSuffixGroup.applyState(earningsState)
    }

    fun setAvailableBalance(maybeBalance: String?) {
        itemDashboardNoStakeChainAvailableBalance.setTextOrHide(maybeBalance)
    }

    fun setStakingTypeBadge(model: StakingTypeModel?) {
        itemDashboardNoStakeStakingType.setModelOrHide(model)
    }

    fun unbind() {
        itemDashboardNoStakeChainIcon.clear()
    }
}
