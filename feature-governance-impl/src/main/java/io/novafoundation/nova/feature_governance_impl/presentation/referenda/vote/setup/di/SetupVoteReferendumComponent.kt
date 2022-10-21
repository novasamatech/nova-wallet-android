package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.SetupVoteReferendumFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.SetupVoteReferendumPayload

@Subcomponent(
    modules = [
        SetupVoteReferendumModule::class
    ]
)
@ScreenScope
interface SetupVoteReferendumComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SetupVoteReferendumPayload,
        ): SetupVoteReferendumComponent
    }

    fun inject(fragment: SetupVoteReferendumFragment)
}
