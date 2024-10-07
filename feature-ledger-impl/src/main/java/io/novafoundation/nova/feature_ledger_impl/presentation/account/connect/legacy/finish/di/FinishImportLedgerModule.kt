package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.finish.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.LegacyLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.legacy.finish.FinishImportLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.legacy.finish.RealFinishImportLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.finish.FinishImportLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.finish.FinishImportLedgerViewModel

@Module(includes = [ViewModelModule::class])
class FinishImportLedgerModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        legacyLedgerAddAccountRepository: LegacyLedgerAddAccountRepository,
        accountRepository: AccountRepository
    ): FinishImportLedgerInteractor = RealFinishImportLedgerInteractor(legacyLedgerAddAccountRepository, accountRepository)

    @Provides
    @IntoMap
    @ViewModelKey(FinishImportLedgerViewModel::class)
    fun provideViewModel(
        router: LedgerRouter,
        resourceManager: ResourceManager,
        payload: FinishImportLedgerPayload,
        accountInteractor: AccountInteractor,
        interactor: FinishImportLedgerInteractor
    ): ViewModel {
        return FinishImportLedgerViewModel(
            router = router,
            resourceManager = resourceManager,
            payload = payload,
            accountInteractor = accountInteractor,
            interactor = interactor
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): FinishImportLedgerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(FinishImportLedgerViewModel::class.java)
    }
}
