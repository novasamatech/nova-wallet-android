package io.novafoundation.nova.feature_account_impl.presentation.account.list.switching.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithBalanceListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.switching.SwitchWalletViewModel

@Module(includes = [ViewModelModule::class])
class SwitchWalletModule {

    @Provides
    @IntoMap
    @ViewModelKey(SwitchWalletViewModel::class)
    fun provideViewModel(
        accountInteractor: AccountInteractor,
        router: AccountRouter,
        accountListingMixinFactory: MetaAccountWithBalanceListingMixinFactory,
        metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry,
        rootScope: RootScope
    ): ViewModel {
        return SwitchWalletViewModel(
            accountInteractor = accountInteractor,
            router = router,
            accountListingMixinFactory = accountListingMixinFactory,
            metaAccountsUpdatesRegistry = metaAccountsUpdatesRegistry,
            rootScope = rootScope
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SwitchWalletViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SwitchWalletViewModel::class.java)
    }
}
