package io.novafoundation.nova.feature_account_impl.presentation.node.list.di

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
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.node.list.NodesViewModel
import io.novafoundation.nova.feature_account_impl.presentation.node.mixin.api.NodeListingMixin
import io.novafoundation.nova.feature_account_impl.presentation.node.mixin.impl.NodeListingProvider

@Module(includes = [ViewModelModule::class])
class NodesModule {

    @Provides
    fun provideNodeListingMixin(
        interactor: AccountInteractor,
        resourceManager: ResourceManager
    ): NodeListingMixin = NodeListingProvider(interactor, resourceManager)

    @Provides
    @IntoMap
    @ViewModelKey(NodesViewModel::class)
    fun provideViewModel(
        interactor: AccountInteractor,
        router: AccountRouter,
        nodeListingMixin: NodeListingMixin,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager
    ): ViewModel {
        return NodesViewModel(interactor, router, nodeListingMixin, addressIconGenerator, resourceManager)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): NodesViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(NodesViewModel::class.java)
    }
}
