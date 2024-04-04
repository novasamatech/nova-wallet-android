package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.restoreBackup.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.createPassword.CreateCloudBackupPasswordInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.CreateCloudBackupPasswordPayload
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.CreateCloudBackupPasswordViewModel
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.restoreBackup.RestoreCloudBackupViewModel

@Module(includes = [ViewModelModule::class])
class RestoreCloudBackupModule {

    @Provides
    @IntoMap
    @ViewModelKey(RestoreCloudBackupViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        resourceManager: ResourceManager,
        interactor: CreateCloudBackupPasswordInteractor,
        actionBottomSheetLauncher: ActionBottomSheetLauncher
    ): ViewModel {
        return RestoreCloudBackupViewModel(
            router,
            resourceManager,
            interactor,
            actionBottomSheetLauncher
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
