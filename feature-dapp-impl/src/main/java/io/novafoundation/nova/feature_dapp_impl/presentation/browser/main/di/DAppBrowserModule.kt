package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.data.repository.PhishingSitesRepository
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.DappBrowserInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserViewModel
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.RuntimeVersionsRepository

@Module(includes = [ViewModelModule::class])
class DAppBrowserModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        runtimeVersionsRepository: RuntimeVersionsRepository,
        phishingSitesRepository: PhishingSitesRepository,
    ) = DappBrowserInteractor(
        chainRegistry = chainRegistry,
        accountRepository = accountRepository,
        phishingSitesRepository = phishingSitesRepository,
        runtimeVersionsRepository = runtimeVersionsRepository
    )

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): DAppBrowserViewModel {
        return ViewModelProvider(fragment, factory).get(DAppBrowserViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(DAppBrowserViewModel::class)
    fun provideViewModel(
        router: DAppRouter,
        polkadotJsExtensionFactory: PolkadotJsExtensionFactory,
        interactor: DappBrowserInteractor,
        resourceManager: ResourceManager,
        commonInteractor: DappInteractor,
        addressIconGenerator: AddressIconGenerator,
        selectedAccountUseCase: SelectedAccountUseCase,
        signRequester: DAppSignCommunicator,
        searchRequester: DAppSearchCommunicator,
        initialUrl: String
    ): ViewModel {
        return DAppBrowserViewModel(
            router = router,
            polkadotJsExtensionFactory = polkadotJsExtensionFactory,
            interactor = interactor,
            resourceManager = resourceManager,
            addressIconGenerator = addressIconGenerator,
            selectedAccountUseCase = selectedAccountUseCase,
            commonInteractor = commonInteractor,
            signRequester = signRequester,
            dAppSearchRequester = searchRequester,
            initialUrl = initialUrl
        )
    }
}
