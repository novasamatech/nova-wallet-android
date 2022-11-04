package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload

@Subcomponent(
    modules = [
        ReferendumDetailsModule::class
    ]
)
@ScreenScope
interface ReferendumDetailsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ReferendumDetailsPayload,
        ): ReferendumDetailsComponent
    }

    fun inject(fragment: ReferendumDetailsFragment)
}
