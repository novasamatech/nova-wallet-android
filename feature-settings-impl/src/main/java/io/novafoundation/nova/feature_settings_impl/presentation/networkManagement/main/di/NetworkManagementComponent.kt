package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.main.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.main.NetworkManagementFragment

@Subcomponent(
    modules = [
        NetworkManagementModule::class
    ]
)
@ScreenScope
interface NetworkManagementComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): NetworkManagementComponent
    }

    fun inject(fragment: NetworkManagementFragment)
}
