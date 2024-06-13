package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.start.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.start.StartImportLedgerViewModel

@Module(includes = [ViewModelModule::class])
class StartImportLedgerModule {

    @Provides
    @IntoMap
    @ViewModelKey(StartImportLedgerViewModel::class)
    fun provideViewModel(
        router: LedgerRouter,
        appLinksProvider: AppLinksProvider,
        ledgerMigrationTracker: LedgerMigrationTracker,
    ): ViewModel {
        return StartImportLedgerViewModel(router, appLinksProvider, ledgerMigrationTracker)
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): StartImportLedgerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StartImportLedgerViewModel::class.java)
    }
}
