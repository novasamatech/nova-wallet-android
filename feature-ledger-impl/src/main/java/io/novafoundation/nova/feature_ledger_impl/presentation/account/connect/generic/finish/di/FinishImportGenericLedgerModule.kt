package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.finish.di

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
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.GenericLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.generic.finish.FinishImportGenericLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.generic.finish.RealFinishImportGenericLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.finish.FinishImportGenericLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.finish.FinishImportGenericLedgerViewModel

@Module(includes = [ViewModelModule::class])
class FinishImportGenericLedgerModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        genericLedgerAddAccountRepository: GenericLedgerAddAccountRepository,
        accountRepository: AccountRepository,
    ): FinishImportGenericLedgerInteractor = RealFinishImportGenericLedgerInteractor(
        genericLedgerAddAccountRepository = genericLedgerAddAccountRepository,
        accountRepository = accountRepository,
    )

    @Provides
    @IntoMap
    @ViewModelKey(FinishImportGenericLedgerViewModel::class)
    fun provideViewModel(
        router: LedgerRouter,
        resourceManager: ResourceManager,
        payload: FinishImportGenericLedgerPayload,
        accountInteractor: AccountInteractor,
        interactor: FinishImportGenericLedgerInteractor
    ): ViewModel {
        return FinishImportGenericLedgerViewModel(
            router = router,
            resourceManager = resourceManager,
            payload = payload,
            accountInteractor = accountInteractor,
            interactor = interactor
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): FinishImportGenericLedgerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(FinishImportGenericLedgerViewModel::class.java)
    }
}
