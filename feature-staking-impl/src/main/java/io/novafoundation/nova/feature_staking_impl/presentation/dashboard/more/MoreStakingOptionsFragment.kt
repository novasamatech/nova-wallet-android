package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more

import androidx.recyclerview.widget.ConcatAdapter

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentMoreStakingOptionsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.list.DashboardNoStakeAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.list.DashboardSectionAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.list.StakingDAppsDecoration
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.list.StakingDappsAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.model.StakingDAppModel

class MoreStakingOptionsFragment :
    BaseFragment<MoreStakingOptionsViewModel, FragmentMoreStakingOptionsBinding>(),
    DashboardNoStakeAdapter.Handler,
    StakingDappsAdapter.Handler {

    override fun createBinding() = FragmentMoreStakingOptionsBinding.inflate(layoutInflater)

    private val noStakeAdapter = DashboardNoStakeAdapter(this)

    private val sectionAdapter = DashboardSectionAdapter(R.string.staking_dashboard_browser_stake_header)

    private val dAppAdapter = StakingDappsAdapter(this)
    private val dAppLoadingAdapter = CustomPlaceholderAdapter(R.layout.layout_dapps_shimmering)

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
        binder.moreStakingOptionsToolbar.applyStatusBarInsets()
        binder.moreStakingOptionsToolbar.setHomeButtonListener { viewModel.goBack() }

        with(binder.moreStakingOptionsContent) {
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
            noStakeAdapter.submitListPreservingViewPoint(stakingOptionsModel.inAppStaking, binder.moreStakingOptionsContent)

            when (val browserStakingState = stakingOptionsModel.browserStaking) {
                is ExtendedLoadingState.Loaded -> {
                    dAppLoadingAdapter.show(false)
                    dAppAdapter.submitList(browserStakingState.data)
                }
                else -> {
                    dAppLoadingAdapter.show(true)
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
