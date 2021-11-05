package io.novafoundation.nova.feature_crowdloan_impl.presentation.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeManager
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.CrowdloanInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.main.CrowdloanViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorMixin

@Module(includes = [ViewModelModule::class])
class CrowdloanModule {

    @Provides
    @IntoMap
    @ViewModelKey(CrowdloanViewModel::class)
    fun provideViewModel(
        interactor: CrowdloanInteractor,
        resourceManager: ResourceManager,
        iconGenerator: AddressIconGenerator,
        crowdloanSharedState: CrowdloanSharedState,
        router: CrowdloanRouter,
        crowdloanUpdateSystem: UpdateSystem,
        assetSelectorFactory: MixinFactory<AssetSelectorMixin.Presentation>,
        customDialogDisplayer: CustomDialogDisplayer.Presentation,
        customContributeManager: CustomContributeManager,
    ): ViewModel {
        return CrowdloanViewModel(
            interactor,
            iconGenerator,
            resourceManager,
            crowdloanSharedState,
            router,
            customContributeManager,
            crowdloanUpdateSystem,
            assetSelectorFactory,
            customDialogDisplayer
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): CrowdloanViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(CrowdloanViewModel::class.java)
    }
}
