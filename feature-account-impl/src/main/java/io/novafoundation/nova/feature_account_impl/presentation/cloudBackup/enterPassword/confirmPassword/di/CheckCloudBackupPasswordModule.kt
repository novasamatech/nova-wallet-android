package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.confirmPassword.di

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
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.enterPassword.EnterCloudBackupInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.confirmPassword.CheckCloudBackupPasswordViewModel

@Module(includes = [ViewModelModule::class])
class CheckCloudBackupPasswordModule {

    @Provides
    @IntoMap
    @ViewModelKey(CheckCloudBackupPasswordViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        resourceManager: ResourceManager,
        interactor: EnterCloudBackupInteractor,
        actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
    ): ViewModel {
        return CheckCloudBackupPasswordViewModel(
            router,
            resourceManager,
            interactor,
            actionBottomSheetLauncherFactory,
            actionAwaitableMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): CheckCloudBackupPasswordViewModel {
        return ViewModelProvider(
            fragment,
            viewModelFactory
        ).get(CheckCloudBackupPasswordViewModel::class.java)
    }
}
