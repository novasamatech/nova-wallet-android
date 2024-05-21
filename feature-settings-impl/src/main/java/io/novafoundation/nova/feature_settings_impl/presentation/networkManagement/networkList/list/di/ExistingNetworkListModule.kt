package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.list.di

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
import io.novafoundation.nova.feature_settings_impl.domain.NetworkManagementInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.main.NetworkManagementViewModel
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListAdapterItemFactory
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListViewModel
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.list.ExistingNetworkListViewModel

@Module(includes = [ViewModelModule::class])
class ExistingNetworkListModule {

    @Provides
    @IntoMap
    @ViewModelKey(ExistingNetworkListViewModel::class)
    fun provideViewModel(
        networkManagementInteractor: NetworkManagementInteractor,
        networkListAdapterItemFactory: NetworkListAdapterItemFactory,
        router: SettingsRouter,
        resourceManager: ResourceManager
    ): ViewModel {
        return ExistingNetworkListViewModel(
            networkManagementInteractor,
            networkListAdapterItemFactory,
            router,
            resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ExistingNetworkListViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ExistingNetworkListViewModel::class.java)
    }
}
