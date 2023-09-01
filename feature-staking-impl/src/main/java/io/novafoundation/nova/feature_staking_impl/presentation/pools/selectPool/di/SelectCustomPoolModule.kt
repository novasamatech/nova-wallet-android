package io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool.di

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
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.di.staking.startMultiStaking.MultiStakingSelectionStoreProviderKey
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettingsProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool.SelectCustomPoolViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.common.CustomValidatorsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.ChangeStakingReviewValidatorsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.EmptyReviewValidatorsFlowAction
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.ReviewCustomValidatorsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.ReviewValidatorsFlowAction
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.ReviewValidatorsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.SetupStakingReviewValidatorsFlowAction
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.SetupStakingReviewValidatorsRouter
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase

@Module(includes = [ViewModelModule::class])
class SelectCustomPoolModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectCustomPoolViewModel::class)
    fun provideViewModel(stakingRouter: StakingRouter): ViewModel {
        return SelectCustomPoolViewModel(stakingRouter)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SelectCustomPoolViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectCustomPoolViewModel::class.java)
    }
}
