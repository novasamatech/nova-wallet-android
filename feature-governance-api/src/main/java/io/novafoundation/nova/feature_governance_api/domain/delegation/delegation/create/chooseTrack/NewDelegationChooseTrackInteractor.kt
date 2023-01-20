package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack

import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack.model.ChooseTrackData
import kotlinx.coroutines.flow.Flow

interface NewDelegationChooseTrackInteractor {

    fun observeChooseTrackData(): Flow<ChooseTrackData>
}
