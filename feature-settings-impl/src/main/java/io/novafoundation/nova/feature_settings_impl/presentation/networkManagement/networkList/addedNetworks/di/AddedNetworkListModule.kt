package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.addedNetworks.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.NetworkManagementInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListAdapterItemFactory
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.addedNetworks.AddedNetworkListViewModel

@Module(includes = [ViewModelModule::class])
class AddedNetworkListModule {

    @Provides
    @IntoMap
    @ViewModelKey(AddedNetworkListViewModel::class)
    fun provideViewModel(
        networkManagementInteractor: NetworkManagementInteractor,
        networkListAdapterItemFactory: NetworkListAdapterItemFactory,
        appLinksProvider: AppLinksProvider,
        router: SettingsRouter
    ): ViewModel {
        return AddedNetworkListViewModel(
            networkManagementInteractor,
            networkListAdapterItemFactory,
            appLinksProvider,
            router
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): AddedNetworkListViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AddedNetworkListViewModel::class.java)
    }
}
