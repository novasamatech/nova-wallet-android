package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.governanceTracks.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksRequester
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.ChooseTrackInteractor
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.governanceTracks.SelectGovernanceTracksViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SelectGovernanceTracksModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectGovernanceTracksViewModel::class)
    fun provideViewModel(
        interactor: ChooseTrackInteractor,
        trackFormatter: TrackFormatter,
        resourceManager: ResourceManager,
        router: GovernanceRouter,
        payload: SelectTracksRequester.Request,
        chainRegistry: ChainRegistry,
        selectTracksCommunicator: SelectTracksCommunicator
    ): ViewModel {
        return SelectGovernanceTracksViewModel(
            interactor = interactor,
            trackFormatter = trackFormatter,
            resourceManager = resourceManager,
            router = router,
            payload = payload,
            responder = selectTracksCommunicator,
            chainRegistry = chainRegistry
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): SelectGovernanceTracksViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectGovernanceTracksViewModel::class.java)
    }
}
