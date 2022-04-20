package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.data.repository.FavouritesDAppRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.PhishingSitesRepository
import io.novafoundation.nova.feature_dapp_impl.domain.browser.DappBrowserInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserViewModel
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator
import io.novafoundation.nova.feature_dapp_impl.web3.states.ExtensionStoreFactory

@Module(includes = [ViewModelModule::class])
class DAppBrowserModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        phishingSitesRepository: PhishingSitesRepository,
        favouritesDAppRepository: FavouritesDAppRepository,
    ) = DappBrowserInteractor(
        phishingSitesRepository = phishingSitesRepository,
        favouritesDAppRepository = favouritesDAppRepository
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
        interactor: DappBrowserInteractor,
        selectedAccountUseCase: SelectedAccountUseCase,
        signRequester: DAppSignCommunicator,
        searchRequester: DAppSearchCommunicator,
        initialUrl: String,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        extensionStoreFactory: ExtensionStoreFactory,
    ): ViewModel {
        return DAppBrowserViewModel(
            router = router,
            interactor = interactor,
            selectedAccountUseCase = selectedAccountUseCase,
            signRequester = signRequester,
            dAppSearchRequester = searchRequester,
            initialUrl = initialUrl,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            extensionStoreFactory = extensionStoreFactory
        )
    }
}
