package io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.feature_account_impl.domain.startCreateWallet.StartCreateWalletInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet.StartCreateWalletPayload
import io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet.StartCreateWalletViewModel

@Module(includes = [ViewModelModule::class])
class StartCreateWalletModule {

    @Provides
    @IntoMap
    @ViewModelKey(StartCreateWalletViewModel::class)
    fun provideViewModel(
        accountRouter: AccountRouter,
        resourceManager: ResourceManager,
        startCreateWalletInteractor: StartCreateWalletInteractor,
        actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory,
        customDialogProvider: CustomDialogDisplayer.Presentation,
        payload: StartCreateWalletPayload
    ): ViewModel {
        return StartCreateWalletViewModel(
            accountRouter,
            resourceManager,
            startCreateWalletInteractor,
            actionBottomSheetLauncherFactory,
            payload,
            customDialogProvider
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): StartCreateWalletViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StartCreateWalletViewModel::class.java)
    }
}
