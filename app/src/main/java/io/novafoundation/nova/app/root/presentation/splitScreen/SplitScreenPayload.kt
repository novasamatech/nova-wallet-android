package io.novafoundation.nova.app.root.presentation.splitScreen

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.DelayedNavigation
import kotlinx.android.parcel.Parcelize

sealed interface SplitScreenPayload : Parcelable {

    @Parcelize
    object NoNavigation : SplitScreenPayload

    @Parcelize
    class InstantNavigationOnAttach(
        val delayedNavigation: DelayedNavigation
    ) : SplitScreenPayload
}
