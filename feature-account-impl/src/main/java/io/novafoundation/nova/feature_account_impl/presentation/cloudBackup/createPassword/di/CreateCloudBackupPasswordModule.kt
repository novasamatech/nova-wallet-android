package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.di

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
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.createPassword.CreateCloudBackupPasswordInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.CreateCloudBackupPasswordPayload
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.CreateCloudBackupPasswordViewModel

@Module(includes = [ViewModelModule::class])
class CreateCloudBackupPasswordModule {

    @Provides
    @IntoMap
    @ViewModelKey(CreateCloudBackupPasswordViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        resourceManager: ResourceManager,
        interactor: CreateCloudBackupPasswordInteractor,
        actionBottomSheetLauncher: ActionBottomSheetLauncher,
        payload: CreateCloudBackupPasswordPayload,
        accountInteractor: AccountInteractor
    ): ViewModel {
        return CreateCloudBackupPasswordViewModel(
            router,
            resourceManager,
            interactor,
            actionBottomSheetLauncher,
            payload,
            accountInteractor
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): CreateCloudBackupPasswordViewModel {
        return ViewModelProvider(
            fragment,
            viewModelFactory
        ).get(CreateCloudBackupPasswordViewModel::class.java)
    }
}
