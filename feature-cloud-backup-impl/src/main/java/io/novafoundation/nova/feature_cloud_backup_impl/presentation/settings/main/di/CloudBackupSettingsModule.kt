package io.novafoundation.nova.feature_cloud_backup_impl.presentation.settings.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_cloud_backup_impl.domain.settings.CloudBackupSettingsInteractor
import io.novafoundation.nova.feature_cloud_backup_impl.presentation.CloudBackupRouter
import io.novafoundation.nova.feature_cloud_backup_impl.presentation.settings.main.CloudBackupSettingsViewModel

@Module(includes = [ViewModelModule::class])
class CloudBackupSettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(CloudBackupSettingsViewModel::class)
    fun provideViewModel(
        cloudBackupRouter: CloudBackupRouter,
        cloudBackupSettingsInteractor: CloudBackupSettingsInteractor
    ): ViewModel {
        return CloudBackupSettingsViewModel(cloudBackupRouter, cloudBackupSettingsInteractor)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): CloudBackupSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(CloudBackupSettingsViewModel::class.java)
    }
}
