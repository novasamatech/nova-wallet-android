package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_impl.domain.manualBackup.ManualBackupSelectAccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.ManualBackupSelectAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.ManualBackupSelectAccountViewModel

@Module(includes = [ViewModelModule::class])
class ManualBackupSelectAccountModule {

    @Provides
    @IntoMap
    @ViewModelKey(ManualBackupSelectAccountViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        resourceManager: ResourceManager,
        manualBackupSelectAccountInteractor: ManualBackupSelectAccountInteractor,
        walletUiUseCase: WalletUiUseCase,
        payload: ManualBackupSelectAccountPayload
    ): ViewModel {
        return ManualBackupSelectAccountViewModel(
            router,
            resourceManager,
            manualBackupSelectAccountInteractor,
            walletUiUseCase,
            payload
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ManualBackupSelectAccountViewModel {
        return ViewModelProvider(
            fragment,
            viewModelFactory
        ).get(ManualBackupSelectAccountViewModel::class.java)
    }
}
