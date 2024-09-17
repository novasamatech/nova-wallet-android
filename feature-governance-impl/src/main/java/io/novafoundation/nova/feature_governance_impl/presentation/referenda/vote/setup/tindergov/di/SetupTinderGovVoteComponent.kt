package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVotePayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.referenda.di.SetupTinderGovVoteModule
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.SetupTinderGovVoteFragment

@Subcomponent(
    modules = [
        SetupTinderGovVoteModule::class
    ]
)
@ScreenScope
interface SetupTinderGovVoteComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SetupVotePayload,
        ): SetupTinderGovVoteComponent
    }

    fun inject(fragment: SetupTinderGovVoteFragment)
}
