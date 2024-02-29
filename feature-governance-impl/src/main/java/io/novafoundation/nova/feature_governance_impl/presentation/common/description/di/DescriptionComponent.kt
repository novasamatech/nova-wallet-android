package io.novafoundation.nova.feature_governance_impl.presentation.common.description.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.common.description.DescriptionFragment
import io.novafoundation.nova.feature_governance_api.presentation.referenda.common.description.DescriptionPayload

@Subcomponent(
    modules = [
        DescriptionModule::class
    ]
)
@ScreenScope
interface DescriptionComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: DescriptionPayload,
        ): DescriptionComponent
    }

    fun inject(fragment: DescriptionFragment)
}
