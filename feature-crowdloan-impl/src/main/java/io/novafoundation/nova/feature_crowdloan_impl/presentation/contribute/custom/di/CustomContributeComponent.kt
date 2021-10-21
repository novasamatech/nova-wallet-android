package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeFragment
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.model.CustomContributePayload

@Subcomponent(
    modules = [
        CustomContributeModule::class
    ]
)
@ScreenScope
interface CustomContributeComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: CustomContributePayload
        ): CustomContributeComponent
    }

    fun inject(fragment: CustomContributeFragment)
}
