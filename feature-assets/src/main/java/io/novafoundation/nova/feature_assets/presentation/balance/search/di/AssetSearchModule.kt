package io.novafoundation.nova.feature_assets.presentation.balance.search.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractor
import io.novafoundation.nova.feature_assets.presentation.WalletRouter
import io.novafoundation.nova.feature_assets.presentation.balance.search.AssetSearchViewModel
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class AssetSearchModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry
    ) = AssetSearchInteractor(walletRepository, accountRepository, chainRegistry)

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): AssetSearchViewModel {
        return ViewModelProvider(fragment, factory).get(AssetSearchViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(AssetSearchViewModel::class)
    fun provideViewModel(
        router: WalletRouter,
        interactor: AssetSearchInteractor,
    ): ViewModel {
        return AssetSearchViewModel(
            router = router,
            interactor = interactor
        )
    }
}
