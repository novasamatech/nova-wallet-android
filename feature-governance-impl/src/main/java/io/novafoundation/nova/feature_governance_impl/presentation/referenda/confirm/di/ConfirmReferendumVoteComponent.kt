package io.novafoundation.nova.feature_governance_impl.presentation.referenda.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.confirm.ConfirmReferendumVoteFragment

@Subcomponent(
    modules = [
        ConfirmReferendumVoteModule::class
    ]
)
@ScreenScope
interface ConfirmReferendumVoteComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): ConfirmReferendumVoteComponent
    }

    fun inject(fragment: ConfirmReferendumVoteFragment)
}
