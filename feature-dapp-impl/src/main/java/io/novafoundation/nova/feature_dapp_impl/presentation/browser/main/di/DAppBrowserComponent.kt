package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserFragment
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload

@Subcomponent(
    modules = [
        DAppBrowserModule::class
    ]
)
@ScreenScope
interface DAppBrowserComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: DAppBrowserPayload
        ): DAppBrowserComponent
    }

    fun inject(fragment: DAppBrowserFragment)
}
