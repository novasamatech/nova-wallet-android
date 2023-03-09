package io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.select.di

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
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.staking.rewardDestination.ChangeRewardDestinationInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.rewardDestination.RewardDestinationValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination.RewardDestinationMixin
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.select.SelectRewardDestinationViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class SelectRewardDestinationModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectRewardDestinationViewModel::class)
    fun provideViewModel(
        interactor: StakingInteractor,
        router: StakingRouter,
        resourceManager: ResourceManager,
        changeRewardDestinationInteractor: ChangeRewardDestinationInteractor,
        validationSystem: RewardDestinationValidationSystem,
        validationExecutor: ValidationExecutor,
        rewardDestinationMixin: RewardDestinationMixin.Presentation,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        selectedAssetSharedState: StakingSharedState,
        stakingSharedComputation: StakingSharedComputation,
    ): ViewModel {
        return SelectRewardDestinationViewModel(
            router,
            interactor,
            resourceManager,
            changeRewardDestinationInteractor,
            validationSystem,
            validationExecutor,
            feeLoaderMixin,
            rewardDestinationMixin,
            selectedAssetSharedState,
            stakingSharedComputation
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): SelectRewardDestinationViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectRewardDestinationViewModel::class.java)
    }
}
