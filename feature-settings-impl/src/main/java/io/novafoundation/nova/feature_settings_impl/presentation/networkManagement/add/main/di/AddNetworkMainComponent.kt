package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main.AddNetworkMainFragment

@Subcomponent(
    modules = [
        AddNetworkMainModule::class
    ]
)
@ScreenScope
interface AddNetworkMainComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): AddNetworkMainComponent
    }

    fun inject(fragment: AddNetworkMainFragment)
}
