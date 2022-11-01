package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model

import androidx.core.view.isGone
import coil.ImageLoader
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.feature_governance_impl.presentation.view.NovaChipView

data class ReferendumTrackModel(val name: String, val icon: Icon, val sameWithOther: Boolean)

fun NovaChipView.setReferendumTrackModel(
    maybeTrack: ReferendumTrackModel?,
    imageLoader: ImageLoader
) = letOrHide(maybeTrack) { track ->
    isGone = track.sameWithOther
    setText(track.name)
    setIcon(track.icon, imageLoader)
}
