package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.CloudBackupSettingsInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.CloudBackupSettingsViewModel

@Module(includes = [ViewModelModule::class])
class CloudBackupSettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(CloudBackupSettingsViewModel::class)
    fun provideViewModel(
        resourceManager: ResourceManager,
        router: SettingsRouter,
        cloudBackupSettingsInteractor: CloudBackupSettingsInteractor,
        syncWalletsBackupPasswordCommunicator: SyncWalletsBackupPasswordCommunicator
    ): ViewModel {
        return CloudBackupSettingsViewModel(
            resourceManager,
            router,
            cloudBackupSettingsInteractor,
            syncWalletsBackupPasswordCommunicator
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): CloudBackupSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(CloudBackupSettingsViewModel::class.java)
    }
}
