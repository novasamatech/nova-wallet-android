package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.ConfirmReferendumVoteFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.ConfirmVoteReferendumPayload
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.confirm.ConfirmTinderGovVoteFragment

@Subcomponent(
    modules = [
        ConfirmTinderGovVoteModule::class
    ]
)
@ScreenScope
interface ConfirmTinderGovVoteComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): ConfirmTinderGovVoteComponent
    }

    fun inject(fragment: ConfirmTinderGovVoteFragment)
}
