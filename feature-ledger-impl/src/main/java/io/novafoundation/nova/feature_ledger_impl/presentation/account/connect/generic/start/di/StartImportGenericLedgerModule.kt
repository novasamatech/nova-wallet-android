package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.start.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.start.StartImportGenericLedgerViewModel

@Module(includes = [ViewModelModule::class])
class StartImportGenericLedgerModule {

    @Provides
    @IntoMap
    @ViewModelKey(StartImportGenericLedgerViewModel::class)
    fun provideViewModel(
        router: LedgerRouter,
        appLinksProvider: AppLinksProvider,
    ): ViewModel {
        return StartImportGenericLedgerViewModel(router, appLinksProvider)
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): StartImportGenericLedgerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StartImportGenericLedgerViewModel::class.java)
    }
}
