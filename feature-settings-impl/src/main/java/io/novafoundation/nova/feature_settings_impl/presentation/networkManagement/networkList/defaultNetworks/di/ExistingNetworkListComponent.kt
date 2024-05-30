package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.defaultNetworks.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.addedNetworks.di.AddedNetworkListComponent
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.addedNetworks.di.AddedNetworkListModule
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.defaultNetworks.ExistingNetworkListFragment

@Subcomponent(
    modules = [
        ExistingNetworkListModule::class
    ]
)
@ScreenScope
interface ExistingNetworkListComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): ExistingNetworkListComponent
    }

    fun inject(fragment: ExistingNetworkListFragment)
}
