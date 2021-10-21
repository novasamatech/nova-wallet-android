package io.novafoundation.nova.feature_staking_impl.presentation.staking.balance.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.UnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.balance.ManageStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.balance.SYSTEM_MANAGE_STAKING_BOND_MORE
import io.novafoundation.nova.feature_staking_impl.domain.validations.balance.SYSTEM_MANAGE_STAKING_REBOND
import io.novafoundation.nova.feature_staking_impl.domain.validations.balance.SYSTEM_MANAGE_STAKING_REDEEM
import io.novafoundation.nova.feature_staking_impl.domain.validations.balance.SYSTEM_MANAGE_STAKING_UNBOND
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.balance.StakingBalanceViewModel
import javax.inject.Named

@Module(includes = [ViewModelModule::class])
class StakingBalanceModule {

    @Provides
    @IntoMap
    @ViewModelKey(StakingBalanceViewModel::class)
    fun provideViewModel(
        stakingInteractor: StakingInteractor,
        @Named(SYSTEM_MANAGE_STAKING_REDEEM) redeemValidationSystem: ManageStakingValidationSystem,
        @Named(SYSTEM_MANAGE_STAKING_UNBOND) unbondValidationSystem: ManageStakingValidationSystem,
        @Named(SYSTEM_MANAGE_STAKING_BOND_MORE) bondMoreValidationSystem: ManageStakingValidationSystem,
        @Named(SYSTEM_MANAGE_STAKING_REBOND) rebondValidationSystem: ManageStakingValidationSystem,
        unbondingInteractor: UnbondInteractor,
        validationExecutor: ValidationExecutor,
        resourceManager: ResourceManager,
        router: StakingRouter,
    ): ViewModel {
        return StakingBalanceViewModel(
            router,
            redeemValidationSystem,
            unbondValidationSystem,
            bondMoreValidationSystem,
            rebondValidationSystem,
            validationExecutor,
            unbondingInteractor,
            resourceManager,
            stakingInteractor,
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): StakingBalanceViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StakingBalanceViewModel::class.java)
    }
}
