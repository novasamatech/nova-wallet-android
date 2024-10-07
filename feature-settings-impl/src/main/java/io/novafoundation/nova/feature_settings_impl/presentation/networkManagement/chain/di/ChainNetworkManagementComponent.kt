package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.ChainNetworkManagementFragment
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.ChainNetworkManagementPayload

@Subcomponent(
    modules = [
        ChainNetworkManagementModule::class
    ]
)
@ScreenScope
interface ChainNetworkManagementComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ChainNetworkManagementPayload
        ): ChainNetworkManagementComponent
    }

    fun inject(fragment: ChainNetworkManagementFragment)
}
