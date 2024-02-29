package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.chooseTracks

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.ChooseTrackInteractor
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.revoke.chooseTracks.RevokeDelegationChooseTracksPayload
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.revoke.confirm.RevokeDelegationConfirmPayload
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.delegationTracks.SelectDelegationTracksViewModel
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

class RevokeDelegationChooseTracksViewModel(
    interactor: ChooseTrackInteractor,
    trackFormatter: TrackFormatter,
    governanceSharedState: GovernanceSharedState,
    resourceManager: ResourceManager,
    private val router: GovernanceRouter,
    private val payload: RevokeDelegationChooseTracksPayload
) : SelectDelegationTracksViewModel(
    interactor = interactor,
    trackFormatter = trackFormatter,
    governanceSharedState = governanceSharedState,
    resourceManager = resourceManager,
    router = router,
    chooseTrackDataFlow = interactor.observeRevokeDelegationTrackData(payload.delegateId)
) {

    override val showDescription = false

    override val title: Flow<String> = flowOf {
        resourceManager.getString(R.string.select_delegation_tracks_revoke_title)
    }.shareInBackground()

    override fun nextClicked(trackIds: List<BigInteger>) {
        val nextPayload = RevokeDelegationConfirmPayload(payload.delegateId, trackIds)
        router.openRevokeDelegationsConfirm(nextPayload)
    }
}
