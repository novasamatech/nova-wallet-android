package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import android.view.View
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.insets.applyNavigationBarInsets
import io.novafoundation.nova.common.utils.insets.applyStatusBarInsets
import io.novafoundation.nova.common.view.setModelOrHide
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
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

    override fun createBinding() = FragmentStakingBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun applyInsets(rootView: View) {
        binder.stakingToolbar.applyStatusBarInsets()
        binder.root.applyNavigationBarInsets(consume = false)
    }

    override fun initViews() {
        binder.stakingToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.stakingMigrationAlert.setOnCloseClickListener { viewModel.closeMigrationAlert() }
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
        observeBrowserEvents(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)

        viewModel.migrationAlertFlow.observe { binder.stakingMigrationAlert.setModelOrHide(it) }

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
