package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.settings.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettingsProviderFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.settings.CustomValidatorsSettingsViewModel
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase

@Module(includes = [ViewModelModule::class])
class CustomValidatorsSettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(CustomValidatorsSettingsViewModel::class)
    fun provideViewModel(
        recommendationSettingsProviderFactory: RecommendationSettingsProviderFactory,
        router: StakingRouter,
        tokenUseCase: TokenUseCase
    ): ViewModel {
        return CustomValidatorsSettingsViewModel(
            router,
            recommendationSettingsProviderFactory,
            tokenUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): CustomValidatorsSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(CustomValidatorsSettingsViewModel::class.java)
    }
}
