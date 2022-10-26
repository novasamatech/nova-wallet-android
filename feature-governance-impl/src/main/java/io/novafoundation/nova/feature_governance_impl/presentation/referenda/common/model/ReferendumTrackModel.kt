package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model

import coil.ImageLoader
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.feature_governance_impl.presentation.view.NovaChipView

data class ReferendumTrackModel(val name: String, val icon: Icon)

fun NovaChipView.setReferendumTrackModel(
    maybeTrack: ReferendumTrackModel?,
    imageLoader: ImageLoader
) = letOrHide(maybeTrack) { track ->
    setText(track.name)
    setIcon(track.icon, imageLoader)
}
