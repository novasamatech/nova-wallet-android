package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.syncWallets.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.createPassword.CreateCloudBackupPasswordInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.syncWallets.SyncWalletsBackupPasswordViewModel

@Module(includes = [ViewModelModule::class])
class SyncWalletsBackupPasswordModule {

    @Provides
    @IntoMap
    @ViewModelKey(SyncWalletsBackupPasswordViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        resourceManager: ResourceManager,
        interactor: CreateCloudBackupPasswordInteractor,
        actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory,
        syncWalletsBackupPasswordCommunicator: SyncWalletsBackupPasswordCommunicator
    ): ViewModel {
        return SyncWalletsBackupPasswordViewModel(
            router,
            resourceManager,
            interactor,
            actionBottomSheetLauncherFactory,
            syncWalletsBackupPasswordCommunicator
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SyncWalletsBackupPasswordViewModel {
        return ViewModelProvider(
            fragment,
            viewModelFactory
        ).get(SyncWalletsBackupPasswordViewModel::class.java)
    }
}
