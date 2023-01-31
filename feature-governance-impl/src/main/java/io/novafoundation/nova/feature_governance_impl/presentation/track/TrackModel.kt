package io.novafoundation.nova.feature_governance_impl.presentation.track

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.feature_governance_impl.presentation.view.NovaChipView

class TrackModel(val name: String, val icon: Icon)

fun NovaChipView.setTrackModel(trackModel: TrackModel?) = letOrHide(trackModel) { track ->
    setText(track.name)
    setIcon(track.icon)
}
