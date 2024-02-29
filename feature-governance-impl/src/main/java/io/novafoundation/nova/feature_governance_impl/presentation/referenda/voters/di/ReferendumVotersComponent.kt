package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.ReferendumVotersFragment
import io.novafoundation.nova.feature_governance_api.presentation.referenda.voters.ReferendumVotersPayload

@Subcomponent(
    modules = [
        ReferendumVotersModule::class
    ]
)
@ScreenScope
interface ReferendumVotersComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance parcelable: ReferendumVotersPayload,
        ): ReferendumVotersComponent
    }

    fun inject(fragment: ReferendumVotersFragment)
}
