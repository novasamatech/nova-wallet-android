package io.novafoundation.nova.feature_account_impl.presentation.account.list.delegationUpdates.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.delegated.DelegatedMetaAccountUpdatesListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.delegationUpdates.DelegatedAccountUpdatesViewModel

@Module(includes = [ViewModelModule::class])
class DelegatedAccountUpdatesModule {

    @Provides
    @IntoMap
    @ViewModelKey(DelegatedAccountUpdatesViewModel::class)
    fun provideViewModel(
        delegatedMetaAccountUpdatesListingMixinFactory: DelegatedMetaAccountUpdatesListingMixinFactory,
        accountRouter: AccountRouter,
        appLinksProvider: AppLinksProvider,
        accountInteractor: AccountInteractor
    ): ViewModel {
        return DelegatedAccountUpdatesViewModel(
            delegatedMetaAccountUpdatesListingMixinFactory,
            accountRouter,
            appLinksProvider,
            accountInteractor
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): DelegatedAccountUpdatesViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(DelegatedAccountUpdatesViewModel::class.java)
    }
}
