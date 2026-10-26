package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink

import android.net.Uri
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.CancellationException

private const val STAKING_DASHBOARD_DEEP_LINK_PREFIX = "/open/staking"

class StakingDashboardDeepLinkHandler(
    private val stakingRouter: StakingRouter,
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val targetResolver: StakingDeepLinkTargetResolver
) : DeepLinkHandler {

    override val callbackFlow: Flow<CallbackEvent> = emptyFlow()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false
        return path == STAKING_DASHBOARD_DEEP_LINK_PREFIX
    }

    override suspend fun handleDeepLink(data: Uri) = runCatching {
        automaticInteractionGate.awaitInteractionAllowed()
        val target = targetResolver.resolve(data.parameterIgnoreCase("chainId"), data.parameterIgnoreCase("assetId"))

        stakingRouter.openStakingDashboard(target)
    }.onFailure {
        if (it is CancellationException) throw it
    }

    private fun Uri.parameterIgnoreCase(name: String): String? {
        val key = queryParameterNames.firstOrNull { it.equals(name, ignoreCase = true) } ?: return null
        return getQueryParameter(key)
    }
}
