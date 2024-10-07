package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.settings.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.progress.ProgressDialogMixinFactory
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.common.view.input.selector.ListSelectorMixin
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.MetaAccountTypePresentationMapper
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.CloudBackupSettingsInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.settings.BackupSettingsViewModel

@Module(includes = [ViewModelModule::class])
class CloudBackupSettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(BackupSettingsViewModel::class)
    fun provideViewModel(
        resourceManager: ResourceManager,
        router: SettingsRouter,
        accountInteractor: AccountInteractor,
        cloudBackupSettingsInteractor: CloudBackupSettingsInteractor,
        syncWalletsBackupPasswordCommunicator: SyncWalletsBackupPasswordCommunicator,
        changeBackupPasswordCommunicator: ChangeBackupPasswordCommunicator,
        restoreBackupPasswordCommunicator: RestoreBackupPasswordCommunicator,
        actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory,
        accountTypePresentationMapper: MetaAccountTypePresentationMapper,
        walletUiUseCase: WalletUiUseCase,
        progressDialogMixinFactory: ProgressDialogMixinFactory,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        listSelectorMixinFactory: ListSelectorMixin.Factory,
        customDialogProvider: CustomDialogDisplayer.Presentation
    ): ViewModel {
        return BackupSettingsViewModel(
            resourceManager,
            router,
            accountInteractor,
            cloudBackupSettingsInteractor,
            syncWalletsBackupPasswordCommunicator,
            changeBackupPasswordCommunicator,
            restoreBackupPasswordCommunicator,
            actionBottomSheetLauncherFactory,
            accountTypePresentationMapper,
            walletUiUseCase,
            progressDialogMixinFactory,
            actionAwaitableMixinFactory,
            listSelectorMixinFactory,
            customDialogProvider
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
