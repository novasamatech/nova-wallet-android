package io.novafoundation.nova.feature_account_impl.presentation.language.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.language.LanguagesViewModel

@Module(includes = [ViewModelModule::class])
class LanguagesModule {

    @Provides
    @IntoMap
    @ViewModelKey(LanguagesViewModel::class)
    fun provideViewModel(
        interactor: AccountInteractor,
        router: AccountRouter
    ): ViewModel {
        return LanguagesViewModel(interactor, router)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): LanguagesViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(LanguagesViewModel::class.java)
    }
}
