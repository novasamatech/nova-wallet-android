package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.governanceTracks

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksRequester
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksResponder
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.fromTrackIds
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.toTrackIds
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.ChooseTrackInteractor
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.BaseSelectTracksViewModel
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SelectGovernanceTracksViewModel(
    interactor: ChooseTrackInteractor,
    trackFormatter: TrackFormatter,
    resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val router: GovernanceRouter,
    private val payload: SelectTracksRequester.Request,
    private val responder: SelectTracksResponder
) : BaseSelectTracksViewModel(
    trackFormatter = trackFormatter,
    resourceManager = resourceManager,
    router = router,
    chooseTrackDataFlow = interactor.observeTracksByChain(payload.chainId)
) {

    val chainModel = flowOf { chainRegistry.getChain(payload.chainId) }
        .map { mapChainToUi(it) }

    init {
        selectedTracksFlow.value = payload.selectedTracks.toTrackIds()
    }

    override suspend fun getChainAsset(): Chain.Asset {
        return chainRegistry.getChain(payload.chainId).utilityAsset
    }

    override fun backClicked() {
        launch {
            responder.respond(SelectTracksResponder.Response(payload.chainId, selectedTracksFlow.value.fromTrackIds()))

            super.backClicked()
        }
    }
}
