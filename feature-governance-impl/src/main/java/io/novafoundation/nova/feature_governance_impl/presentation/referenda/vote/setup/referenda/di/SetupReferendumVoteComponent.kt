package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.referenda.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.referenda.SetupReferendumVoteFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVotePayload

@Subcomponent(
    modules = [
        SetupReferendumVoteModule::class
    ]
)
@ScreenScope
interface SetupReferendumVoteComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SetupVotePayload,
        ): SetupReferendumVoteComponent
    }

    fun inject(fragment: SetupReferendumVoteFragment)
}
