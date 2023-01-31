package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack.NewDelegationChooseTrackInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select.SelectDelegationTracksPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select.SelectDelegationTracksViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter

@Module(includes = [ViewModelModule::class])
class SelectDelegationTracksModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectDelegationTracksViewModel::class)
    fun provideViewModel(
        newDelegationChooseTrackInteractor: NewDelegationChooseTrackInteractor,
        trackFormatter: TrackFormatter,
        governanceSharedState: GovernanceSharedState,
        resourceManager: ResourceManager,
        router: GovernanceRouter,
        payload: SelectDelegationTracksPayload
    ): ViewModel {
        return SelectDelegationTracksViewModel(
            newDelegationChooseTrackInteractor = newDelegationChooseTrackInteractor,
            trackFormatter = trackFormatter,
            governanceSharedState = governanceSharedState,
            resourceManager = resourceManager,
            router = router,
            payload = payload
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): SelectDelegationTracksViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectDelegationTracksViewModel::class.java)
    }
}
