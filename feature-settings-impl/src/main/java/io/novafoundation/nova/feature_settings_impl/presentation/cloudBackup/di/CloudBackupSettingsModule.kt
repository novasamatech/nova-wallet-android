package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.progress.ProgressDialogMixin
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.input.selector.ListSelectorMixin
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.CloudBackupSettingsInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.BackupSettingsViewModel

@Module(includes = [ViewModelModule::class])
class CloudBackupSettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(BackupSettingsViewModel::class)
    fun provideViewModel(
        resourceManager: ResourceManager,
        router: SettingsRouter,
        cloudBackupSettingsInteractor: CloudBackupSettingsInteractor,
        syncWalletsBackupPasswordCommunicator: SyncWalletsBackupPasswordCommunicator,
        changeBackupPasswordCommunicator: ChangeBackupPasswordCommunicator,
        actionBottomSheetLauncher: ActionBottomSheetLauncher,
        progressDialogMixin: ProgressDialogMixin,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        listSelectorMixinFactory: ListSelectorMixin.Factory
    ): ViewModel {
        return BackupSettingsViewModel(
            resourceManager,
            router,
            cloudBackupSettingsInteractor,
            syncWalletsBackupPasswordCommunicator,
            changeBackupPasswordCommunicator,
            actionBottomSheetLauncher,
            progressDialogMixin,
            actionAwaitableMixinFactory,
            listSelectorMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): BackupSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(BackupSettingsViewModel::class.java)
    }
}
