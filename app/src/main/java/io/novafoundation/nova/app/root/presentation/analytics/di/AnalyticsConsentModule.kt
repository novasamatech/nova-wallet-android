package io.novafoundation.nova.app.root.presentation.analytics.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.analytics.AnalyticsOptOutManager
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.app.root.presentation.analytics.AnalyticsConsentFragment
import io.novafoundation.nova.app.root.presentation.analytics.AnalyticsConsentViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule

@Module(
    includes = [
        ViewModelModule::class
    ]
)
class AnalyticsConsentModule {

    @Provides
    @IntoMap
    @ViewModelKey(AnalyticsConsentViewModel::class)
    fun provideViewModel(
        analyticsOptOutManager: AnalyticsOptOutManager,
        appLinksProvider: AppLinksProvider,
        router: RootRouter,
        fragment: Fragment
    ): ViewModel {
        return AnalyticsConsentViewModel(
            analyticsOptOutManager,
            appLinksProvider,
            router,
            AnalyticsConsentFragment.nextNavigation(fragment)
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): AnalyticsConsentViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AnalyticsConsentViewModel::class.java)
    }
}
