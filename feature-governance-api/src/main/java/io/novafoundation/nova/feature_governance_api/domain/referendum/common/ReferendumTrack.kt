package io.novafoundation.nova.feature_governance_api.domain.referendum.common

import io.novafoundation.nova.feature_governance_api.domain.track.Track

data class ReferendumTrack(val track: Track, val sameWithOther: Boolean)
