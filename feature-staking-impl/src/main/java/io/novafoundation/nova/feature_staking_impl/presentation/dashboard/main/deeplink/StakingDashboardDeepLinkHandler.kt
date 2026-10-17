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
private const val CHAIN_ID_PARAM = "chainId"
private const val ASSET_ID_PARAM = "assetId"

/**
 * `/open/staking` opens the staking dashboard. With `chainId` (and an optional `assetId`, default 0)
 * the dashboard then continues into staking for that asset: details for an existing stake,
 * otherwise the start-staking flow.
 */
class StakingDashboardDeepLinkHandler(
    private val stakingRouter: StakingRouter,
    private val targetResolver: StakingDeepLinkTargetResolver,
    private val pendingNavigation: StakingDashboardPendingNavigation,
    private val automaticInteractionGate: AutomaticInteractionGate
) : DeepLinkHandler {

    override val callbackFlow: Flow<CallbackEvent> = emptyFlow()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false
        return path.startsWith(STAKING_DASHBOARD_DEEP_LINK_PREFIX)
    }

    override suspend fun handleDeepLink(data: Uri) = runCatching {
        automaticInteractionGate.awaitInteractionAllowed()

        val target = targetResolver.resolve(
            chainIdParam = data.getQueryParameterIgnoreCase(CHAIN_ID_PARAM),
            assetIdParam = data.getQueryParameterIgnoreCase(ASSET_ID_PARAM)
        )

        target?.let(pendingNavigation::set)

        stakingRouter.openStakingDashboard()
    }

    private fun Uri.getQueryParameterIgnoreCase(name: String): String? {
        val actualName = queryParameterNames.firstOrNull { it.equals(name, ignoreCase = true) } ?: return null

        return getQueryParameter(actualName)
    }
}
