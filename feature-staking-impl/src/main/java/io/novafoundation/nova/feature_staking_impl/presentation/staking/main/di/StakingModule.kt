package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdateSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.StakingViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components.ComponentsModule
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorFactory

@Module(includes = [ViewModelModule::class, ComponentsModule::class])
class StakingModule {

    @Provides
    @IntoMap
    @ViewModelKey(StakingViewModel::class)
    fun provideViewModel(
        selectedAccountUseCase: SelectedAccountUseCase,

        assetSelectorMixinFactory: AssetSelectorFactory,
        alertsComponentFactory: AlertsComponentFactory,
        unbondingComponentFactory: UnbondingComponentFactory,
        startStakingComponentFactory: StartStakingComponentFactory,
        stakeSummaryComponentFactory: StakeSummaryComponentFactory,
        userRewardsComponentFactory: UserRewardsComponentFactory,
        stakeActionsComponentFactory: StakeActionsComponentFactory,
        networkInfoComponentFactory: NetworkInfoComponentFactory,

        router: StakingRouter,

        validationExecutor: ValidationExecutor,
        stakingUpdateSystem: StakingUpdateSystem,
    ): ViewModel {
        return StakingViewModel(
            selectedAccountUseCase = selectedAccountUseCase,
            assetSelectorMixinFactory = assetSelectorMixinFactory,
            alertsComponentFactory = alertsComponentFactory,
            unbondingComponentFactory = unbondingComponentFactory,
            startStakingComponentFactory = startStakingComponentFactory,
            stakeSummaryComponentFactory = stakeSummaryComponentFactory,
            userRewardsComponentFactory = userRewardsComponentFactory,
            stakeActionsComponentFactory = stakeActionsComponentFactory,
            networkInfoComponentFactory = networkInfoComponentFactory,
            router = router,
            validationExecutor = validationExecutor,
            stakingUpdateSystem = stakingUpdateSystem,
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): StakingViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StakingViewModel::class.java)
    }
}
