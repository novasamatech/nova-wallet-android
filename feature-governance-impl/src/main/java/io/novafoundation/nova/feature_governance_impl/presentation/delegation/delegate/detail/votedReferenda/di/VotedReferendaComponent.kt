package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.votedReferenda.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.votedReferenda.VotedReferendaFragment
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.votedReferenda.VotedReferendaPayload

@Subcomponent(
    modules = [
        VotedReferendaModule::class
    ]
)
@ScreenScope
interface VotedReferendaComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: VotedReferendaPayload
        ): VotedReferendaComponent
    }

    fun inject(fragment: VotedReferendaFragment)
}
