package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model

import androidx.core.view.isGone
import coil.ImageLoader
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.feature_governance_impl.presentation.common.models.TrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.view.NovaChipView

data class ReferendumTrackModel(val details: TrackModel, val sameWithOther: Boolean) {
    val name: String
        get() = details.name

    val icon: Icon
        get() = details.icon
}

fun NovaChipView.setReferendumTrackModel(
    maybeTrack: ReferendumTrackModel?,
    imageLoader: ImageLoader
) = letOrHide(maybeTrack) { track ->
    isGone = track.sameWithOther
    setText(track.name)
    setIcon(track.icon, imageLoader)
}
