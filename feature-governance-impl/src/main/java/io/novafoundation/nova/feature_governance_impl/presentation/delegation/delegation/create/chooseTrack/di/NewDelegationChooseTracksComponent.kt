package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseTrack.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseTrack.NewDelegationChooseTracksFragment
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.create.chooseTrack.NewDelegationChooseTracksPayload

@Subcomponent(
    modules = [
        NewDelegationChooseTracksModule::class
    ]
)
@ScreenScope
interface NewDelegationChooseTracksComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: NewDelegationChooseTracksPayload
        ): NewDelegationChooseTracksComponent
    }

    fun inject(fragment: NewDelegationChooseTracksFragment)
}
