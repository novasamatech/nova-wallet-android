package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.preConfiguredNetworks.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.NetworkManagementInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListAdapterItemFactory
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.defaultNetworks.ExistingNetworkListViewModel
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.preConfiguredNetworks.PreConfiguredNetworksViewModel

@Module(includes = [ViewModelModule::class])
class PreConfiguredNetworksModule {

    @Provides
    @IntoMap
    @ViewModelKey(PreConfiguredNetworksViewModel::class)
    fun provideViewModel(
        networkManagementInteractor: NetworkManagementInteractor,
        networkListAdapterItemFactory: NetworkListAdapterItemFactory,
        router: SettingsRouter
    ): ViewModel {
        return PreConfiguredNetworksViewModel(
            networkManagementInteractor,
            networkListAdapterItemFactory,
            router
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): PreConfiguredNetworksViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PreConfiguredNetworksViewModel::class.java)
    }
}
