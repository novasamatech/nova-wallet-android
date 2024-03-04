package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.chooseTracks.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.ChooseTrackInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.revoke.chooseTracks.RevokeDelegationChooseTracksPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.chooseTracks.RevokeDelegationChooseTracksViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter

@Module(includes = [ViewModelModule::class])
class RevokeDelegationChooseTracksModule {

    @Provides
    @IntoMap
    @ViewModelKey(RevokeDelegationChooseTracksViewModel::class)
    fun provideViewModel(
        interactor: ChooseTrackInteractor,
        trackFormatter: TrackFormatter,
        governanceSharedState: GovernanceSharedState,
        resourceManager: ResourceManager,
        router: GovernanceRouter,
        payload: RevokeDelegationChooseTracksPayload
    ): ViewModel {
        return RevokeDelegationChooseTracksViewModel(
            interactor = interactor,
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
    ): RevokeDelegationChooseTracksViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(RevokeDelegationChooseTracksViewModel::class.java)
    }
}
