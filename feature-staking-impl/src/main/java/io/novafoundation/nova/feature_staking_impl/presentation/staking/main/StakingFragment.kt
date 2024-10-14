package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentStakingBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.setupAlertsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.setupNetworkInfoComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.setupStakeActionsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.setupStakeSummaryComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.setupUnbondingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.setupUserRewardsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.setupYourPoolComponent

import javax.inject.Inject

class StakingFragment : BaseFragment<StakingViewModel, FragmentStakingBinding>() {

    override val binder by viewBinding(FragmentStakingBinding::bind)

    @Inject
    protected lateinit var imageLoader: ImageLoader

    override fun initViews() {
        binder.stakingToolbar.applyStatusBarInsets()
        binder.stakingToolbar.setHomeButtonListener { viewModel.backClicked() }
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
        setupExternalActions(viewModel)

        setupNetworkInfoComponent(viewModel.networkInfoComponent, binder.stakingNetworkInfo)
        setupStakeSummaryComponent(viewModel.stakeSummaryComponent, binder.stakingStakeSummary)
        setupUserRewardsComponent(viewModel.userRewardsComponent, binder.stakingUserRewards, viewModel.router)
        setupUnbondingComponent(viewModel.unbondingComponent, binder.stakingStakeUnbondings)
        setupStakeActionsComponent(viewModel.stakeActionsComponent, binder.stakingStakeManage)
        setupAlertsComponent(viewModel.alertsComponent, binder.stakingAlertsInfo)
        setupYourPoolComponent(viewModel.yourPoolComponent, binder.stakingYourPool)

        viewModel.titleFlow.observe(binder.stakingToolbar::setTitle)
    }
}
