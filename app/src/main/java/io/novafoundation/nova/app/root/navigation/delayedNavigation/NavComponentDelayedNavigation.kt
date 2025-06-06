package io.novafoundation.nova.app.root.navigation.delayedNavigation

import android.os.Bundle
import io.novafoundation.nova.common.navigation.DelayedNavigation
import kotlinx.parcelize.Parcelize

@Parcelize
class NavComponentDelayedNavigation(val globalActionId: Int, val extras: Bundle? = null) : DelayedNavigation
