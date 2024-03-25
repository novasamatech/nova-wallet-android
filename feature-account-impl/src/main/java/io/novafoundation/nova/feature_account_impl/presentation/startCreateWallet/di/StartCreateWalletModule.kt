package io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet.StartCreateWalletViewModel

@Module(includes = [ViewModelModule::class])
class StartCreateWalletModule {

    @Provides
    @IntoMap
    @ViewModelKey(StartCreateWalletViewModel::class)
    fun provideViewModel(
        accountRouter: AccountRouter,
        resourceManager: ResourceManager
    ): ViewModel {
        return StartCreateWalletViewModel(
            accountRouter,
            resourceManager
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
