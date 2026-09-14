package io.novafoundation.nova.app.root.presentation.analytics.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.app.root.presentation.analytics.AnalyticsConsentFragment
import io.novafoundation.nova.common.di.scope.ScreenScope

@Subcomponent(
    modules = [
        AnalyticsConsentModule::class
    ]
)
@ScreenScope
interface AnalyticsConsentComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): AnalyticsConsentComponent
    }

    fun inject(fragment: AnalyticsConsentFragment)
}
