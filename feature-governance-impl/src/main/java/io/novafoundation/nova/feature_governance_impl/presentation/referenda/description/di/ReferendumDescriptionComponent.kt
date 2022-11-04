package io.novafoundation.nova.feature_governance_impl.presentation.referenda.description.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.description.ReferendumDescriptionFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.description.ReferendumDescriptionPayload

@Subcomponent(
    modules = [
        ReferendumDescriptionModule::class
    ]
)
@ScreenScope
interface ReferendumDescriptionComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ReferendumDescriptionPayload,
        ): ReferendumDescriptionComponent
    }

    fun inject(fragment: ReferendumDescriptionFragment)
}
