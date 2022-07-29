package io.novafoundation.nova.feature_account_impl.presentation.account.select.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.select.AccountSwitchViewModel

@Module(includes = [ViewModelModule::class])
class AccountSwitchModule {

    @Provides
    @IntoMap
    @ViewModelKey(AccountSwitchViewModel::class)
    fun provideViewModel(
        accountInteractor: AccountInteractor,
        router: AccountRouter,
        accountListingMixinFactory: MetaAccountListingMixinFactory,
    ): ViewModel {
        return AccountSwitchViewModel(
            accountInteractor = accountInteractor,
            router = router,
            accountListingMixinFactory = accountListingMixinFactory,
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): AccountSwitchViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AccountSwitchViewModel::class.java)
    }
}
