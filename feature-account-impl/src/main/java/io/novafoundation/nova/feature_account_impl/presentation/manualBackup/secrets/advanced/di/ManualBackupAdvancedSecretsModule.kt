package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.advanced.di

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
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.advanced.ManualBackupAdvancedSecretsViewModel
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.ManualBackupSecretsAdapterItemFactory

@Module(includes = [ViewModelModule::class])
class ManualBackupAdvancedSecretsModule {

    @Provides
    @IntoMap
    @ViewModelKey(ManualBackupAdvancedSecretsViewModel::class)
    fun provideViewModel(
        resourceManager: ResourceManager,
        router: AccountRouter,
        payload: ManualBackupCommonPayload,
        secretsAdapterItemFactory: ManualBackupSecretsAdapterItemFactory
    ): ViewModel {
        return ManualBackupAdvancedSecretsViewModel(
            resourceManager,
            router,
            payload,
            secretsAdapterItemFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ManualBackupAdvancedSecretsViewModel {
        return ViewModelProvider(
            fragment,
            viewModelFactory
        ).get(ManualBackupAdvancedSecretsViewModel::class.java)
    }
}
