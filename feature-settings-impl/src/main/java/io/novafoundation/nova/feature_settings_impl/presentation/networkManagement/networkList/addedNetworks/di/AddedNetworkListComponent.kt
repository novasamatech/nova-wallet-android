package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.addedNetworks.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.addedNetworks.AddedNetworkListFragment

@Subcomponent(
    modules = [
        AddedNetworkListModule::class
    ]
)
@ScreenScope
interface AddedNetworkListComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): AddedNetworkListComponent
    }

    fun inject(fragment: AddedNetworkListFragment)
}
