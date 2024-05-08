package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.di

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
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.ManualBackupSecretsAdapterItemFactory
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.ManualBackupSecretsViewModel

@Module(includes = [ViewModelModule::class])
class ManualBackupSecretsModule {

    @Provides
    @IntoMap
    @ViewModelKey(ManualBackupSecretsViewModel::class)
    fun provideViewModel(
        resourceManager: ResourceManager,
        router: AccountRouter,
        payload: ManualBackupCommonPayload,
        secretsAdapterItemFactory: ManualBackupSecretsAdapterItemFactory,
        walletUiUseCase: WalletUiUseCase
    ): ViewModel {
        return ManualBackupSecretsViewModel(
            resourceManager,
            router,
            payload,
            secretsAdapterItemFactory,
            walletUiUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ManualBackupSecretsViewModel {
        return ViewModelProvider(
            fragment,
            viewModelFactory
        ).get(ManualBackupSecretsViewModel::class.java)
    }
}
