package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.ConfirmReferendumVoteFragment
import io.novafoundation.nova.feature_governance_api.presentation.referenda.vote.confirm.ConfirmVoteReferendumPayload

@Subcomponent(
    modules = [
        ConfirmReferendumVoteModule::class
    ]
)
@ScreenScope
interface ConfirmReferendumVoteComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ConfirmVoteReferendumPayload,
        ): ConfirmReferendumVoteComponent
    }

    fun inject(fragment: ConfirmReferendumVoteFragment)
}
