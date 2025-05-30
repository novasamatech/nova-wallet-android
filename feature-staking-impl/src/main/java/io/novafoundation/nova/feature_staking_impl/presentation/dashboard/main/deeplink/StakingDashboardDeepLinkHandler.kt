package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink

import android.net.Uri
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val STAKING_DASHBOARD_DEEP_LINK_PREFIX = "/open/staking"

class StakingDashboardDeepLinkHandler(
    private val stakingRouter: StakingRouter,
    private val automaticInteractionGate: AutomaticInteractionGate
) : DeepLinkHandler {

    override val callbackFlow: Flow<CallbackEvent> = emptyFlow()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false
        return path.startsWith(STAKING_DASHBOARD_DEEP_LINK_PREFIX)
    }

    override suspend fun handleDeepLink(data: Uri) = runCatching {
        automaticInteractionGate.awaitInteractionAllowed()

        stakingRouter.openStakingDashboard()
    }
}
