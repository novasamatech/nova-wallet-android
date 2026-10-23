package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main

import android.view.View
import android.os.Bundle
import androidx.recyclerview.widget.ConcatAdapter

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.onLoaded
import io.novafoundation.nova.common.domain.onNotLoaded
import io.novafoundation.nova.common.utils.insets.applyStatusBarInsets
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentStakingDashboardBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.list.DashboardLoadingAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.list.DashboardNoStakeAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.list.DashboardSectionAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list.DashboardHasStakeAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.announcements.AnnouncementsAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list.DashboardHeaderAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list.MoreStakingOptionsAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink.StakingDeepLinkTarget

class StakingDashboardFragment :
    BaseFragment<StakingDashboardViewModel, FragmentStakingDashboardBinding>(),
    DashboardHasStakeAdapter.Handler,
    DashboardNoStakeAdapter.Handler,
    DashboardHeaderAdapter.Handler,
    MoreStakingOptionsAdapter.Handler {

    companion object {

        private const val DEEP_LINK_TARGET = "deep_link_target"

        fun getBundle(target: StakingDeepLinkTarget?) = Bundle().apply {
            putParcelable(DEEP_LINK_TARGET, target)
        }
    }

    override fun createBinding() = FragmentStakingDashboardBinding.inflate(layoutInflater)

    private val headerAdapter = DashboardHeaderAdapter(this)
    private val announcementsAdapter = AnnouncementsAdapter(onLinkClicked = { viewModel.announcementLinkClicked(it) })
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

    override fun applyInsets(rootView: View) {
        binder.stakingDashboardContent.applyStatusBarInsets()
    }

    override fun initViews() {
        argumentOrNull<StakingDeepLinkTarget>(DEEP_LINK_TARGET)?.let { target ->
            arguments?.remove(DEEP_LINK_TARGET)
            viewModel.setDeepLinkTarget(target)
        }
        binder.stakingDashboardContent.setHasFixedSize(true)

        binder.stakingDashboardContent.adapter = ConcatAdapter(
            headerAdapter,
            announcementsAdapter,
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
        viewModel.deepLinkDestination.observe { destination ->
            destination?.let(viewModel::openDeepLinkDestination)
        }

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

        viewModel.announcements.observe(announcementsAdapter::submitList)

        viewModel.scrollToTopEvent.observeEvent {
            binder.stakingDashboardContent.scrollToPosition(0)
        }
    }

    override fun onResume() {
        super.onResume()
        // As iOS on every dashboard appearance: request the start-staking maximum yield again while there is no stake
        viewModel.onResume()
    }

    override fun onDestroyView() {
        // iOS drops a pending staking link once the dashboard leaves the screen for another tab or a pushed screen
        if (!requireActivity().isChangingConfigurations) viewModel.cancelDeepLink()

        super.onDestroyView()
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
