package io.novafoundation.nova.feature_assets.presentation.novacard.waiting.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.novacard.waiting.WaitingNovaCardTopUpViewModel
import io.novafoundation.nova.feature_assets.presentation.novacard.waiting.flows.TopUpCaseFactory

@Module(includes = [ViewModelModule::class])
class WaitingNovaCardTopUpModule {

    @Provides
    fun provideTopUpCaseFactory(
        assetsRouter: AssetsRouter,
        resourceManager: ResourceManager,
        novaCardInteractor: NovaCardInteractor
    ) = TopUpCaseFactory(
        assetsRouter,
        resourceManager,
        novaCardInteractor
    )

    @Provides
    @IntoMap
    @ViewModelKey(WaitingNovaCardTopUpViewModel::class)
    fun provideViewModel(
        assetsRouter: AssetsRouter,
        novaCardInteractor: NovaCardInteractor,
        topUpCaseFactory: TopUpCaseFactory
    ): ViewModel {
        return WaitingNovaCardTopUpViewModel(assetsRouter, novaCardInteractor, topUpCaseFactory)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): WaitingNovaCardTopUpViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(WaitingNovaCardTopUpViewModel::class.java)
    }
}
