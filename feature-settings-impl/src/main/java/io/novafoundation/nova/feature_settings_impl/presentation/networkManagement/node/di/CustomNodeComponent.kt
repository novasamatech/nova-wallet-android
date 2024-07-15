package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.node.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.node.CustomNodeFragment
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.node.CustomNodePayload

@Subcomponent(
    modules = [
        CustomNodeModule::class
    ]
)
@ScreenScope
interface CustomNodeComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: CustomNodePayload
        ): CustomNodeComponent
    }

    fun inject(fragment: CustomNodeFragment)
}
