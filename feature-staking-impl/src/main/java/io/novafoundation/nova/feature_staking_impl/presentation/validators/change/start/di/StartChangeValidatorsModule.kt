package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.start.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.ValidatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.start.StartChangeValidatorsViewModel

@Module(includes = [ViewModelModule::class])
class StartChangeValidatorsModule {

    @Provides
    @IntoMap
    @ViewModelKey(StartChangeValidatorsViewModel::class)
    fun provideViewModel(
        validatorRecommendatorFactory: ValidatorRecommendatorFactory,
        router: StakingRouter,
        sharedState: SetupStakingSharedState,
        resourceManager: ResourceManager,
        interactor: StakingInteractor
    ): ViewModel {
        return StartChangeValidatorsViewModel(
            router,
            validatorRecommendatorFactory,
            sharedState,
            resourceManager,
            interactor
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): StartChangeValidatorsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StartChangeValidatorsViewModel::class.java)
    }
}
