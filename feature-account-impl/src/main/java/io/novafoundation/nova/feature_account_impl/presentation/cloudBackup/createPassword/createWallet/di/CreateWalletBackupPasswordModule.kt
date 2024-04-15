package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.createWallet.di

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
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.createWallet.CreateBackupPasswordPayload
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.createWallet.CreateWalletBackupPasswordViewModel

@Module(includes = [ViewModelModule::class])
class CreateWalletBackupPasswordModule {

    @Provides
    @IntoMap
    @ViewModelKey(CreateWalletBackupPasswordViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        resourceManager: ResourceManager,
        interactor: CreateCloudBackupPasswordInteractor,
        actionBottomSheetLauncher: ActionBottomSheetLauncher,
        payload: CreateBackupPasswordPayload,
        accountInteractor: AccountInteractor
    ): ViewModel {
        return CreateWalletBackupPasswordViewModel(
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
    ): CreateWalletBackupPasswordViewModel {
        return ViewModelProvider(
            fragment,
            viewModelFactory
        ).get(CreateWalletBackupPasswordViewModel::class.java)
    }
}
