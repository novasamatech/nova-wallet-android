package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main.AddNetworkPayload
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails.AddNetworkFragment

@Subcomponent(
    modules = [
        AddNetworkModule::class
    ]
)
@ScreenScope
interface AddNetworkComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance argument: AddNetworkPayload,
        ): AddNetworkComponent
    }

    fun inject(fragment: AddNetworkFragment)
}
