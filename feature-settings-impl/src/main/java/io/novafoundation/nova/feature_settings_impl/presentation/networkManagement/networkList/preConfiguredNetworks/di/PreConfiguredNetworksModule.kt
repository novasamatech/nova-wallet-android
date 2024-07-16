package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.preConfiguredNetworks.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.coingecko.CoinGeckoLinkParser
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.progress.ProgressDialogMixinFactory
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.PreConfiguredNetworksInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListAdapterItemFactory
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.preConfiguredNetworks.PreConfiguredNetworksViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class PreConfiguredNetworksModule {

    @Provides
    @IntoMap
    @ViewModelKey(PreConfiguredNetworksViewModel::class)
    fun provideViewModel(
        preConfiguredNetworksInteractor: PreConfiguredNetworksInteractor,
        networkListAdapterItemFactory: NetworkListAdapterItemFactory,
        router: SettingsRouter,
        resourceManager: ResourceManager,
        chainRegistry: ChainRegistry,
        progressDialogMixinFactory: ProgressDialogMixinFactory,
        coinGeckoLinkParser: CoinGeckoLinkParser
    ): ViewModel {
        return PreConfiguredNetworksViewModel(
            interactor = preConfiguredNetworksInteractor,
            networkListAdapterItemFactory = networkListAdapterItemFactory,
            router = router,
            resourceManager = resourceManager,
            chainRegistry = chainRegistry,
            progressDialogMixinFactory = progressDialogMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): PreConfiguredNetworksViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PreConfiguredNetworksViewModel::class.java)
    }
}
