package io.novafoundation.nova.app.root.navigation.delayedNavigation

import io.novafoundation.nova.common.navigation.DelayedNavigation
import kotlinx.android.parcel.Parcelize

@Parcelize
class EndFlowDelayedNavigation(
    val returnToAction: Int
) : DelayedNavigation
