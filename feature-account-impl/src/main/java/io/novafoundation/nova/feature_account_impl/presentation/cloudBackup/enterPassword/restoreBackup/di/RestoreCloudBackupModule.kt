package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.restoreBackup.di

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
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.enterPassword.EnterCloudBackupInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.restoreBackup.RestoreCloudBackupViewModel

@Module(includes = [ViewModelModule::class])
class RestoreCloudBackupModule {

    @Provides
    @IntoMap
    @ViewModelKey(RestoreCloudBackupViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        resourceManager: ResourceManager,
        interactor: EnterCloudBackupInteractor,
        accountInteractor: AccountInteractor,
        actionBottomSheetLauncher: ActionBottomSheetLauncher,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
    ): ViewModel {
        return RestoreCloudBackupViewModel(
            accountInteractor,
            router,
            resourceManager,
            interactor,
            actionBottomSheetLauncher,
            actionAwaitableMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): RestoreCloudBackupViewModel {
        return ViewModelProvider(
            fragment,
            viewModelFactory
        ).get(RestoreCloudBackupViewModel::class.java)
    }
}
