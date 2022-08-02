package io.novafoundation.nova.feature_account_impl.presentation.account.create.di

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
import io.novafoundation.nova.feature_account_impl.presentation.account.create.CreateAccountViewModel

@Module(includes = [ViewModelModule::class])
class CreateAccountModule {

    @Provides
    @IntoMap
    @ViewModelKey(CreateAccountViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        resourceManager: ResourceManager,
    ): ViewModel {
        return CreateAccountViewModel(router, resourceManager)
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): CreateAccountViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(CreateAccountViewModel::class.java)
    }
}
