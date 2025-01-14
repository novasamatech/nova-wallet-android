package io.novafoundation.nova.app.root.presentation.splitScreen.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.app.root.domain.SplitScreenInteractor
import io.novafoundation.nova.app.root.presentation.splitScreen.SplitScreenPayload
import io.novafoundation.nova.app.root.presentation.splitScreen.SplitScreenViewModel
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.navigation.DelayedNavigationRouter
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_dapp_api.data.repository.BrowserTabExternalRepository
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter

@Module(
    includes = [
        ViewModelModule::class
    ]
)
class SplitScreenFragmentModule {

    @Provides
    fun provideInteractor(repository: BrowserTabExternalRepository, accountRepository: AccountRepository): SplitScreenInteractor {
        return SplitScreenInteractor(repository, accountRepository)
    }

    @Provides
    @IntoMap
    @ViewModelKey(SplitScreenViewModel::class)
    fun provideViewModel(
        interactor: SplitScreenInteractor,
        dAppRouter: DAppRouter,
        delayedNavigationRouter: DelayedNavigationRouter,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        resourceManager: ResourceManager,
        payload: SplitScreenPayload
    ): ViewModel {
        return SplitScreenViewModel(interactor, dAppRouter, delayedNavigationRouter, actionAwaitableMixinFactory, resourceManager, payload)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SplitScreenViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SplitScreenViewModel::class.java)
    }
}
