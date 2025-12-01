package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.finish.di

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
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.paritySigner.ParitySignerAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.RawKeyAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.SeedAddAccountRepository
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.finish.FinishImportParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.finish.RealFinishImportParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.finish.FinishImportParitySignerViewModel

@Module(includes = [ViewModelModule::class])
class FinishImportParitySignerModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        paritySignerAddAccountRepository: ParitySignerAddAccountRepository,
        rawKeyAddAccountRepository: RawKeyAddAccountRepository,
        seedAddAccountRepository: SeedAddAccountRepository,
        accountRepository: AccountRepository,
        encryptionDefaults: EncryptionDefaults
    ): FinishImportParitySignerInteractor = RealFinishImportParitySignerInteractor(
        paritySignerAddAccountRepository,
        rawKeyAddAccountRepository,
        seedAddAccountRepository,
        accountRepository,
        encryptionDefaults
    )

    @Provides
    @IntoMap
    @ViewModelKey(FinishImportParitySignerViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        resourceManager: ResourceManager,
        payload: ParitySignerAccountPayload,
        accountInteractor: AccountInteractor,
        interactor: FinishImportParitySignerInteractor
    ): ViewModel {
        return FinishImportParitySignerViewModel(
            router = router,
            resourceManager = resourceManager,
            payload = payload,
            accountInteractor = accountInteractor,
            interactor = interactor
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): FinishImportParitySignerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(FinishImportParitySignerViewModel::class.java)
    }
}
