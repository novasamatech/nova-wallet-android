package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.setupAlertsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.setupNetworkInfoComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.setupStakeActionsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.setupStakeSummaryComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.setupStartStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.setupUnbondingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.setupUserRewardsComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.setupAssetSelector
import kotlinx.android.synthetic.main.fragment_staking.stakingAlertsInfo
import kotlinx.android.synthetic.main.fragment_staking.stakingAssetSelector
import kotlinx.android.synthetic.main.fragment_staking.stakingAvatar
import kotlinx.android.synthetic.main.fragment_staking.stakingContainer
import kotlinx.android.synthetic.main.fragment_staking.stakingEstimate
import kotlinx.android.synthetic.main.fragment_staking.stakingNetworkInfo
import kotlinx.android.synthetic.main.fragment_staking.stakingStakeManage
import kotlinx.android.synthetic.main.fragment_staking.stakingStakeSummary
import kotlinx.android.synthetic.main.fragment_staking.stakingStakeUnbondings
import kotlinx.android.synthetic.main.fragment_staking.stakingUserRewards
import javax.inject.Inject

class StakingFragment : BaseFragment<StakingViewModel>() {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_staking, container, false)
    }

    override fun initViews() {
        stakingContainer.applyStatusBarInsets()

        stakingAvatar.setOnClickListener {
            viewModel.avatarClicked()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .stakingComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: StakingViewModel) {
        observeValidations(viewModel)
        setupAssetSelector(stakingAssetSelector, viewModel.assetSelectorMixin, imageLoader)

        setupNetworkInfoComponent(viewModel.networkInfoComponent, stakingNetworkInfo)
        setupStakeSummaryComponent(viewModel.stakeSummaryComponent, stakingStakeSummary)
        setupUserRewardsComponent(viewModel.userRewardsComponent, stakingUserRewards, viewModel.router)
        setupUnbondingComponent(viewModel.unbondingComponent, stakingStakeUnbondings)
        setupStakeActionsComponent(viewModel.stakeActionsComponent, stakingStakeManage)
        setupStartStakingComponent(viewModel.startStakingComponent, stakingEstimate)
        setupAlertsComponent(viewModel.alertsComponent, stakingAlertsInfo)

        viewModel.selectedWalletFlow.observe(stakingAvatar::setModel)
    }
}
