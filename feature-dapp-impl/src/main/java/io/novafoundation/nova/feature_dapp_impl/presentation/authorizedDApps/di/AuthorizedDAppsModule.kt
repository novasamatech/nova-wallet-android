package io.novafoundation.nova.feature_dapp_impl.presentation.authorizedDApps.di

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
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_api.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.domain.authorizedDApps.AuthorizedDAppsInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.authorizedDApps.AuthorizedDAppsViewModel
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session

@Module(includes = [ViewModelModule::class])
class AuthorizedDAppsModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        accountRepository: AccountRepository,
        metadataRepository: DAppMetadataRepository,
        web3Session: Web3Session
    ) = AuthorizedDAppsInteractor(
        accountRepository = accountRepository,
        metadataRepository = metadataRepository,
        web3Session = web3Session
    )

    @Provides
    @ScreenScope
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): AuthorizedDAppsViewModel {
        return ViewModelProvider(fragment, factory).get(AuthorizedDAppsViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(AuthorizedDAppsViewModel::class)
    fun provideViewModel(
        router: DAppRouter,
        interactor: AuthorizedDAppsInteractor,
        walletUiUseCase: WalletUiUseCase,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
    ): ViewModel {
        return AuthorizedDAppsViewModel(
            router = router,
            interactor = interactor,
            walletUiUseCase = walletUiUseCase,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory
        )
    }
}
