package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.start.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.start.StartImportParitySignerViewModel

@Module(includes = [ViewModelModule::class])
class StartImportParitySignerModule {

    @Provides
    @IntoMap
    @ViewModelKey(StartImportParitySignerViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
    ): ViewModel {
        return StartImportParitySignerViewModel(router)
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): StartImportParitySignerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StartImportParitySignerViewModel::class.java)
    }
}
