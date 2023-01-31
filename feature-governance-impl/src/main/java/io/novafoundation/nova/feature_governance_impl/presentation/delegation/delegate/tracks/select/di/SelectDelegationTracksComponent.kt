package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list.DelegateListFragment
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select.SelectDelegationTracksFragment
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select.SelectDelegationTracksPayload

@Subcomponent(
    modules = [
        SelectDelegationTracksModule::class
    ]
)
@ScreenScope
interface SelectDelegationTracksComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SelectDelegationTracksPayload
        ): SelectDelegationTracksComponent
    }

    fun inject(fragment: SelectDelegationTracksFragment)
}
