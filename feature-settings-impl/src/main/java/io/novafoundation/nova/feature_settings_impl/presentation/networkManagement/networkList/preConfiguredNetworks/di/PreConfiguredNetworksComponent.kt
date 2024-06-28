package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.preConfiguredNetworks.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.preConfiguredNetworks.PreConfiguredNetworksFragment

@Subcomponent(
    modules = [
        PreConfiguredNetworksModule::class
    ]
)
@ScreenScope
interface PreConfiguredNetworksComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): PreConfiguredNetworksComponent
    }

    fun inject(fragment: PreConfiguredNetworksFragment)
}
