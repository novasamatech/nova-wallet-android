package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails.di

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
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.AddNetworkInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main.AddNetworkPayload
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails.AddNetworkViewModel
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails.AutofillNetworkMetadataMixinFactory
import io.novafoundation.nova.runtime.ethereum.Web3ApiFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnectionFactory

@Module(includes = [ViewModelModule::class])
class AddNetworkModule {

    @Provides
    fun provideAutofillNetworkMetadataMixinFactory(
        nodeConnectionFactory: NodeConnectionFactory,
        web3ApiFactory: Web3ApiFactory
    ): AutofillNetworkMetadataMixinFactory {
        return AutofillNetworkMetadataMixinFactory(
            nodeConnectionFactory,
            web3ApiFactory
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(AddNetworkViewModel::class)
    fun provideViewModel(
        resourceManager: ResourceManager,
        router: SettingsRouter,
        payload: AddNetworkPayload,
        interactor: AddNetworkInteractor,
        validationExecutor: ValidationExecutor,
        autofillNetworkMetadataMixinFactory: AutofillNetworkMetadataMixinFactory,
        coinGeckoLinkParser: CoinGeckoLinkParser,
        chainRegistry: ChainRegistry,
    ): ViewModel {
        return AddNetworkViewModel(
            resourceManager = resourceManager,
            router = router,
            payload = payload,
            interactor = interactor,
            validationExecutor = validationExecutor,
            autofillNetworkMetadataMixinFactory = autofillNetworkMetadataMixinFactory,
            coinGeckoLinkParser = coinGeckoLinkParser,
            chainRegistry = chainRegistry
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): AddNetworkViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AddNetworkViewModel::class.java)
    }
}
