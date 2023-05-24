package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import coil.clear
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerFrameLayout
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeChainAvailableBalance
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeChainIcon
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeChainName
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeEarnings
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeEarningsShimmer
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeEarningsSuffix
import kotlinx.android.synthetic.main.item_dashboard_no_stake.view.itemDashboardNoStakeEarningsSuffixShimmer

class StakingDashboardNoStakeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ShimmerFrameLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    private val imageLoader: ImageLoader

    init {
        View.inflate(context, R.layout.item_dashboard_no_stake, this)

        background = context.getBlockDrawable().withRippleMask()

        layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(16.dp(context), 8.dp(context), 16.dp(context), 0)
        }

        imageLoader = FeatureUtils.getCommonApi(context).imageLoader()

        val shimmer = Shimmer.AlphaHighlightBuilder()
            .setBaseAlpha(0.6f)
            .build()

        setShimmer(shimmer)
    }

    fun setChainUi(chainUi: ChainUi) {
        itemDashboardNoStakeChainIcon.loadChainIcon(chainUi.icon, imageLoader)
        itemDashboardNoStakeChainName.text = chainUi.name
    }

    fun setEarnings(earningsState: ExtendedLoadingState<String>) {
        earningsState.applyToView(itemDashboardNoStakeEarnings, itemDashboardNoStakeEarningsShimmer) { earnings ->
            itemDashboardNoStakeEarnings.text = earnings
        }

        earningsState.applyToView(itemDashboardNoStakeEarningsSuffix, itemDashboardNoStakeEarningsSuffixShimmer)
    }

    fun setSyncing(isSyncing: Boolean) {
        if (isSyncing) showShimmer(true) else hideShimmer()
    }

    fun setAvailableBalance(maybeBalance: String?) {
        itemDashboardNoStakeChainAvailableBalance.setTextOrHide(maybeBalance)
    }

    fun unbind() {
        itemDashboardNoStakeChainIcon.clear()
    }
}
