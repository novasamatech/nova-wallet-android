package io.novafoundation.nova.feature_governance_api.domain.referendum.common

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId

data class ReferendumTrack(val id: TrackId, val name: String, val sameWithOther: Boolean)
