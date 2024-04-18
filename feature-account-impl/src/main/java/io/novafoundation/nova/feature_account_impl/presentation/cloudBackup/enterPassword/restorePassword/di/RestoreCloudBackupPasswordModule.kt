package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.restorePassword.di

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
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.enterPassword.EnterCloudBackupInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.restorePassword.RestoreCloudBackupPasswordViewModel

@Module(includes = [ViewModelModule::class])
class RestoreCloudBackupPasswordModule {

    @Provides
    @IntoMap
    @ViewModelKey(RestoreCloudBackupPasswordViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        resourceManager: ResourceManager,
        interactor: EnterCloudBackupInteractor,
        actionBottomSheetLauncher: ActionBottomSheetLauncher,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        restoreBackupPasswordCommunicator: RestoreBackupPasswordCommunicator,
    ): ViewModel {
        return RestoreCloudBackupPasswordViewModel(
            router,
            resourceManager,
            interactor,
            actionBottomSheetLauncher,
            actionAwaitableMixinFactory,
            restoreBackupPasswordCommunicator
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): RestoreCloudBackupPasswordViewModel {
        return ViewModelProvider(
            fragment,
            viewModelFactory
        ).get(RestoreCloudBackupPasswordViewModel::class.java)
    }
}
