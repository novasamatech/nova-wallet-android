package io.novafoundation.nova.feature_account_impl.presentation.account.list.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.AccountListViewModel

@Module(includes = [ViewModelModule::class])
class AccountListModule {

    @Provides
    @IntoMap
    @ViewModelKey(AccountListViewModel::class)
    fun provideViewModel(
        interactor: AccountInteractor,
        router: AccountRouter,
        resourceManager: ResourceManager,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        metaAccountListingMixinFactory: MetaAccountListingMixinFactory
    ): ViewModel {
        return AccountListViewModel(interactor, router, resourceManager, actionAwaitableMixinFactory, metaAccountListingMixinFactory)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): AccountListViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AccountListViewModel::class.java)
    }
}
