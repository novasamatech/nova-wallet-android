package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseTrack

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.ChooseTrackInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model.ChooseTrackData
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.create.chooseTrack.NewDelegationChooseTracksPayload
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountPayload
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.delegationTracks.SelectDelegationTracksViewModel
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

class NewDelegationChooseTracksViewModel(
    interactor: ChooseTrackInteractor,
    trackFormatter: TrackFormatter,
    governanceSharedState: GovernanceSharedState,
    resourceManager: ResourceManager,
    private val router: GovernanceRouter,
    private val payload: NewDelegationChooseTracksPayload
) : SelectDelegationTracksViewModel(
    interactor = interactor,
    trackFormatter = trackFormatter,
    governanceSharedState = governanceSharedState,
    resourceManager = resourceManager,
    router = router,
    chooseTrackDataFlow = interactor.chooseTrackDataFlowFor(payload)
) {

    override val title: Flow<String> = flowOf {
        val titleRes = if (payload.isEditMode) R.string.select_delegation_tracks_edit_title else R.string.select_delegation_tracks_add_title
        resourceManager.getString(titleRes)
    }.shareInBackground()

    override val showDescription = true

    override fun nextClicked(trackIds: List<BigInteger>) {
        val nextPayload = NewDelegationChooseAmountPayload(payload.delegateId, trackIds, isEditMode = payload.isEditMode)
        router.openNewDelegationChooseAmount(nextPayload)
    }
}

private fun ChooseTrackInteractor.chooseTrackDataFlowFor(payload: NewDelegationChooseTracksPayload): Flow<ChooseTrackData> {
    return if (payload.isEditMode) {
        observeEditDelegationTrackData(payload.delegateId)
    } else {
        observeNewDelegationTrackData()
    }
}
