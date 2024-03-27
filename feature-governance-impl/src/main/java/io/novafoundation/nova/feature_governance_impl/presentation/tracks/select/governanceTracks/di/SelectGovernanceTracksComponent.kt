package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.governanceTracks.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksRequester
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.governanceTracks.SelectGovernanceTracksFragment

@Subcomponent(
    modules = [
        SelectGovernanceTracksModule::class
    ]
)
@ScreenScope
interface SelectGovernanceTracksComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SelectTracksRequester.Request
        ): SelectGovernanceTracksComponent
    }

    fun inject(fragment: SelectGovernanceTracksFragment)
}
