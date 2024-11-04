package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main

import androidx.recyclerview.widget.ConcatAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.onLoaded
import io.novafoundation.nova.common.domain.onNotLoaded
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentStakingDashboardBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.list.DashboardLoadingAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.list.DashboardNoStakeAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.list.DashboardSectionAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list.DashboardHasStakeAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list.DashboardHeaderAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list.MoreStakingOptionsAdapter

class StakingDashboardFragment :
    BaseFragment<StakingDashboardViewModel, FragmentStakingDashboardBinding>(),
    DashboardHasStakeAdapter.Handler,
    DashboardNoStakeAdapter.Handler,
    DashboardHeaderAdapter.Handler,
    MoreStakingOptionsAdapter.Handler {

    override fun createBinding() = FragmentStakingDashboardBinding.inflate(layoutInflater)

    private val headerAdapter = DashboardHeaderAdapter(this)
    private val hasStakeLoadingAdapter = DashboardLoadingAdapter(initialNumberOfItems = 1, layout = R.layout.item_dashboard_has_stake_loading)
    private val hasStakeAdapter = DashboardHasStakeAdapter(this)
    private val sectionAdapter = DashboardSectionAdapter(R.string.staking_dashboard_no_stake_header)
    private val noStakeLoadingAdapter = DashboardLoadingAdapter(initialNumberOfItems = 3, layout = R.layout.item_dashboard_loading)
    private val noStakeAdapter = DashboardNoStakeAdapter(this)
    private val moreStakingOptionsAdapter = MoreStakingOptionsAdapter(this)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .dashboardComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun initViews() {
        binder.stakingDashboardContent.applyStatusBarInsets()
        binder.stakingDashboardContent.setHasFixedSize(true)

        binder.stakingDashboardContent.adapter = ConcatAdapter(
            headerAdapter,
            hasStakeLoadingAdapter,
            hasStakeAdapter,
            sectionAdapter,
            noStakeLoadingAdapter,
            noStakeAdapter,
            moreStakingOptionsAdapter
        )

        binder.stakingDashboardContent.itemAnimator = null
    }

    override fun subscribe(viewModel: StakingDashboardViewModel) {
        viewModel.stakingDashboardUiFlow.observe { dashboardLoading ->
            dashboardLoading.onLoaded {
                hasStakeAdapter.submitListPreservingViewPoint(it.hasStakeItems, binder.stakingDashboardContent)
                noStakeAdapter.submitListPreservingViewPoint(it.noStakeItems, binder.stakingDashboardContent)

                hasStakeLoadingAdapter.setLoaded(true)
                noStakeLoadingAdapter.setLoaded(true)
            }.onNotLoaded {
                hasStakeLoadingAdapter.setLoaded(false)
                noStakeLoadingAdapter.setLoaded(false)
            }
        }

        viewModel.walletUi.observe(headerAdapter::setSelectedWallet)

        viewModel.scrollToTopEvent.observeEvent {
            binder.stakingDashboardContent.scrollToPosition(0)
        }
    }

    override fun onHasStakeItemClicked(index: Int) {
        viewModel.onHasStakeItemClicked(index)
    }

    override fun onNoStakeItemClicked(index: Int) {
        viewModel.onNoStakeItemClicked(index)
    }

    override fun avatarClicked() {
        viewModel.avatarClicked()
    }

    override fun onMoreOptionsClicked() {
        viewModel.onMoreOptionsClicked()
    }
}
