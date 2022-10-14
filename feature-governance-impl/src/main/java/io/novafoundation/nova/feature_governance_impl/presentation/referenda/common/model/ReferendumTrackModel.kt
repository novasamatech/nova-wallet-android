package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model

import android.widget.TextView
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.useNonNullOrHide
import io.novafoundation.nova.feature_governance_impl.R

data class ReferendumTrackModel(val name: String, @DrawableRes val iconRes: Int)

fun TextView.setReferendumTrackModel(maybeTrack: ReferendumTrackModel?) = useNonNullOrHide(maybeTrack) { track ->
    text = track.name
    setDrawableStart(track.iconRes, widthInDp = 16, paddingInDp = 4, tint = R.color.white_64)
}
