package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.main.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.main.NetworkManagementListFragment

@Subcomponent(
    modules = [
        NetworkManagementListModule::class
    ]
)
@ScreenScope
interface NetworkManagementListComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): NetworkManagementListComponent
    }

    fun inject(fragment: NetworkManagementListFragment)
}
