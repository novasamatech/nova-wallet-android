package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.ReviewCustomValidatorsViewModel
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase

@Module(includes = [ViewModelModule::class])
class ReviewCustomValidatorsModule {

    @Provides
    @IntoMap
    @ViewModelKey(ReviewCustomValidatorsViewModel::class)
    fun provideViewModel(
        addressIconGenerator: AddressIconGenerator,
        stakingInteractor: StakingInteractor,
        resourceManager: ResourceManager,
        router: StakingRouter,
        setupStakingSharedState: SetupStakingSharedState,
        tokenUseCase: TokenUseCase,
        selectedAssetState: StakingSharedState,
        validationExecutor: ValidationExecutor
    ): ViewModel {
        return ReviewCustomValidatorsViewModel(
            router,
            addressIconGenerator,
            stakingInteractor,
            resourceManager,
            setupStakingSharedState,
            selectedAssetState,
            validationExecutor,
            tokenUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ReviewCustomValidatorsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ReviewCustomValidatorsViewModel::class.java)
    }
}
