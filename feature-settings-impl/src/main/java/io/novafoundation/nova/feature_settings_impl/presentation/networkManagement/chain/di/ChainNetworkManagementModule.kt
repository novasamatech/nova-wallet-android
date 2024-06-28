package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.NetworkManagementChainInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.ChainNetworkManagementPayload
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.ChainNetworkManagementViewModel

@Module(includes = [ViewModelModule::class])
class ChainNetworkManagementModule {

    @Provides
    @IntoMap
    @ViewModelKey(ChainNetworkManagementViewModel::class)
    fun provideViewModel(
        router: SettingsRouter,
        resourceManager: ResourceManager,
        networkManagementChainInteractor: NetworkManagementChainInteractor,
        payload: ChainNetworkManagementPayload
    ): ViewModel {
        return ChainNetworkManagementViewModel(
            router,
            resourceManager,
            networkManagementChainInteractor,
            payload
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ChainNetworkManagementViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ChainNetworkManagementViewModel::class.java)
    }
}
