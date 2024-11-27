package io.novafoundation.nova.feature_dapp_impl.presentation.tab.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.tab.BrowserTabsViewModel
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.BrowserTabService

@Module(includes = [ViewModelModule::class])
class BrowserTabsModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): BrowserTabsViewModel {
        return ViewModelProvider(fragment, factory).get(BrowserTabsViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(BrowserTabsViewModel::class)
    fun provideViewModel(
        router: DAppRouter,
        browserTabService: BrowserTabService,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        searchRequester: DAppSearchCommunicator,
    ): ViewModel {
        return BrowserTabsViewModel(
            router = router,
            browserTabService = browserTabService,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            dAppSearchRequester = searchRequester
        )
    }
}
