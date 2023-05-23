package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list.DashboardHasStakeAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list.DashboardHeaderAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list.DashboardLoadingAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list.DashboardNoStakeAdapter
import kotlinx.android.synthetic.main.fragment_staking_dashboard.stakingDashboardContent

class StakingDashboardFragment :
    BaseFragment<StakingDashboardViewModel>(),
    DashboardHasStakeAdapter.Handler,
    DashboardNoStakeAdapter.Handler,
    DashboardHeaderAdapter.Handler {

    private val headerAdapter = DashboardHeaderAdapter(this)
    private val hasStakeAdapter = DashboardHasStakeAdapter(this)
    private val noStakeAdapter = DashboardNoStakeAdapter(this)
    private val loadingItemsAdapter = DashboardLoadingAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_staking_dashboard, container, false)
    }

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
        stakingDashboardContent.applyStatusBarInsets()
        stakingDashboardContent.setHasFixedSize(true)
        stakingDashboardContent.adapter = ConcatAdapter(headerAdapter, hasStakeAdapter, noStakeAdapter, loadingItemsAdapter)
        stakingDashboardContent.itemAnimator = null
    }

    override fun subscribe(viewModel: StakingDashboardViewModel) {
        viewModel.stakingDashboardFlow.observe {
            hasStakeAdapter.submitListPreservingViewPoint(it.hasStakeItems, stakingDashboardContent)
            noStakeAdapter.submitListPreservingViewPoint(it.noStakeItems, stakingDashboardContent)
            loadingItemsAdapter.setNumberOfLoadingItems(it.resolvingItems)
        }

        viewModel.walletUi.observe(headerAdapter::setSelectedWallet)
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
}
