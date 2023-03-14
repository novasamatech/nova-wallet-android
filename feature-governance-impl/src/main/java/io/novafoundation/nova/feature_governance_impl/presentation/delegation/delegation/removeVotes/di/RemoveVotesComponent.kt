package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes.RemoveVotesFragment
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes.RemoveVotesPayload

@Subcomponent(
    modules = [
        RemoveVotesModule::class
    ]
)
@ScreenScope
interface RemoveVotesComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: RemoveVotesPayload,
        ): RemoveVotesComponent
    }

    fun inject(fragment: RemoveVotesFragment)
}
