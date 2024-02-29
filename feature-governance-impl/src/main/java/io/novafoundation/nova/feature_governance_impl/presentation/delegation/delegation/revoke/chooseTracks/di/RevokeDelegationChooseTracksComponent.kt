package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.chooseTracks.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.chooseTracks.RevokeDelegationChooseTracksFragment
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.revoke.chooseTracks.RevokeDelegationChooseTracksPayload

@Subcomponent(
    modules = [
        RevokeDelegationChooseTracksModule::class
    ]
)
@ScreenScope
interface RevokeDelegationChooseTracksComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: RevokeDelegationChooseTracksPayload
        ): RevokeDelegationChooseTracksComponent
    }

    fun inject(fragment: RevokeDelegationChooseTracksFragment)
}
