package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.list.DashboardNoStakeAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.list.DashboardSectionAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.list.StakingDAppsDecoration
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.list.StakingDappsAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.model.StakingDAppModel
import kotlinx.android.synthetic.main.fragment_more_staking_options.moreStakingOptionsContent
import kotlinx.android.synthetic.main.fragment_more_staking_options.moreStakingOptionsToolbar

class MoreStakingOptionsFragment :
    BaseFragment<MoreStakingOptionsViewModel>(),
    DashboardNoStakeAdapter.Handler,
    StakingDappsAdapter.Handler {

    private val noStakeAdapter = DashboardNoStakeAdapter(this)

    private val sectionAdapter = DashboardSectionAdapter(R.string.staking_dashboard_browser_stake_header)

    private val dAppAdapter = StakingDappsAdapter(this)
    private val dAppLoadingAdapter = CustomPlaceholderAdapter(R.layout.layout_dapps_shimmering)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_more_staking_options, container, false)
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .moreStakingOptionsFactory()
            .create(this)
            .inject(this)
    }

    override fun initViews() {
        moreStakingOptionsToolbar.applyStatusBarInsets()
        moreStakingOptionsToolbar.setHomeButtonListener { viewModel.goBack() }

        with(moreStakingOptionsContent) {
            setHasFixedSize(true)
            adapter = ConcatAdapter(
                noStakeAdapter,
                sectionAdapter,
                dAppAdapter,
                dAppLoadingAdapter
            )
            itemAnimator = null

            addItemDecoration(StakingDAppsDecoration(requireContext()))
        }
    }

    override fun subscribe(viewModel: MoreStakingOptionsViewModel) {
        viewModel.moreStakingOptionsUiFlow.observe { stakingOptionsModel ->
            noStakeAdapter.submitListPreservingViewPoint(stakingOptionsModel.inAppStaking, moreStakingOptionsContent)

            when (val browserStakingState = stakingOptionsModel.browserStaking) {
                is ExtendedLoadingState.Loaded -> {
                    dAppLoadingAdapter.showPlaceholder(false)
                    dAppAdapter.submitList(browserStakingState.data)
                }
                else -> {
                    dAppLoadingAdapter.showPlaceholder(true)
                    dAppAdapter.submitList(listOf())
                }
            }
        }
    }

    override fun onNoStakeItemClicked(index: Int) {
        viewModel.onInAppStakingItemClicked(index)
    }

    override fun onDAppClicked(item: StakingDAppModel) {
        viewModel.onBrowserStakingItemClicked(item)
    }
}
