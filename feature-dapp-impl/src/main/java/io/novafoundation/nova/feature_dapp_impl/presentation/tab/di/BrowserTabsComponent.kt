package io.novafoundation.nova.feature_dapp_impl.presentation.tab.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_dapp_impl.presentation.tab.BrowserTabsFragment

@Subcomponent(
    modules = [
        BrowserTabsModule::class
    ]
)
@ScreenScope
interface BrowserTabsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): BrowserTabsComponent
    }

    fun inject(fragment: BrowserTabsFragment)
}
