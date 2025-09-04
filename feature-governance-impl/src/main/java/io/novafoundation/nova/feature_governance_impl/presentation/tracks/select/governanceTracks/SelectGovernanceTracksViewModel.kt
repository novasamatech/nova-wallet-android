package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.governanceTracks

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksRequester
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksResponder
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.fromTrackIds
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.toTrackIds
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.ChooseTrackInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.BaseSelectTracksViewModel
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SelectGovernanceTracksViewModel(
    interactor: ChooseTrackInteractor,
    trackFormatter: TrackFormatter,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val router: GovernanceRouter,
    private val payload: SelectTracksRequester.Request,
    private val responder: SelectTracksResponder
) : BaseSelectTracksViewModel(
    trackFormatter = trackFormatter,
    resourceManager = resourceManager,
    router = router,
    chooseTrackDataFlow = interactor.observeTracksByChain(payload.chainId, payload.governanceType)
) {

    val chainModel = flowOf { chainRegistry.getChain(payload.chainId) }
        .map { mapChainToUi(it) }

    init {
        selectedTracksFlow.value = payload.selectedTracks.toTrackIds()
    }

    override suspend fun getChainAsset(): Chain.Asset {
        return chainRegistry.getChain(payload.chainId).utilityAsset
    }

    override fun trackClicked(position: Int) {
        launch {
            val track = availableTrackFlow.first()[position]
            val selectedTracks = selectedTracksFlow.value.toggle(track.id)
            if (selectedTracks.size >= payload.minTracks) {
                super.trackClicked(position)
            } else {
                showToast(resourceManager.getQuantityString(R.plurals.governance_select_tracks_min_tracks_error, payload.minTracks, payload.minTracks))
            }
        }
    }

    override fun backClicked() {
        launch {
            responder.respond(SelectTracksResponder.Response(payload.chainId, payload.governanceType, selectedTracksFlow.value.fromTrackIds()))

            super.backClicked()
        }
    }
}
