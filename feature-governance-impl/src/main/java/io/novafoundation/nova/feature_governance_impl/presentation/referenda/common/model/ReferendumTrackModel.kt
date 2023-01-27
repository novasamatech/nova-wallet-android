package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model

import androidx.core.view.isGone
import coil.ImageLoader
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.setTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.view.NovaChipView

data class ReferendumTrackModel(val track: TrackModel, val sameWithOther: Boolean)

fun NovaChipView.setReferendumTrackModel(
    maybeTrack: ReferendumTrackModel?,
    imageLoader: ImageLoader
) {
    setTrackModel(maybeTrack?.track, imageLoader)

    maybeTrack?.sameWithOther?.let {
        isGone = it
    }
}
