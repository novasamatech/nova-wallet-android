package io.novafoundation.nova.feature_dapp_impl.presentation.browser.tabs.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.tabPool.TabPoolService
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.tabs.BrowserTabsViewModel


@Module(includes = [ViewModelModule::class])
class BrowserTabsModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): BrowserTabsViewModel {
        return ViewModelProvider(fragment, factory).get(BrowserTabsViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(BrowserTabsViewModel::class)
    fun provideViewModel(tabPoolService: TabPoolService, router: DAppRouter): ViewModel {
        return BrowserTabsViewModel(tabPoolService = tabPoolService, router = router)
    }
}
