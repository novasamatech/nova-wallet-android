package io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_api.presentation.referenda.full.ReferendumFullDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumFullDetailsFragment

@Subcomponent(
    modules = [
        ReferendumFullDetailsModule::class
    ]
)
@ScreenScope
interface ReferendumFullDetailsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ReferendumFullDetailsPayload,
        ): ReferendumFullDetailsComponent
    }

    fun inject(fragment: ReferendumFullDetailsFragment)
}
