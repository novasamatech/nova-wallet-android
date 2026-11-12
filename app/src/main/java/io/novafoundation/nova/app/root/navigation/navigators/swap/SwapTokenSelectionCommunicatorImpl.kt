package io.novafoundation.nova.app.root.navigation.navigators.swap

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_assets.presentation.swap.asset.AssetSwapFlowFragment
import io.novafoundation.nova.feature_assets.presentation.swap.asset.SwapFlowPayload
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapTokenSelectionCommunicator
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapTokenSelectionCommunicator.Request
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapTokenSelectionCommunicator.Response

class SwapTokenSelectionCommunicatorImpl(registry: NavigationHoldersRegistry) :
    SwapTokenSelectionCommunicator,
    NavStackInterScreenCommunicator<Request, Response>(registry, "swap.token.response", "swap.token.request") {

    override fun openRequest(request: Request) {
        super.openRequest(request)
        val caller = navController.currentDestination!!.id
        val payload = if (request.selectingPay) {
            SwapFlowPayload.ReselectAssetIn(request.oppositeAsset, request)
        } else {
            SwapFlowPayload.ReselectAssetOut(request.oppositeAsset, request)
        }
        navController.navigate(R.id.action_open_swap_token_selection, AssetSwapFlowFragment.getBundle(payload))
        navController.getBackStackEntry(R.id.select_swap_token_nav_graph).savedStateHandle[CALLER] = caller
    }

    override fun respond(response: Response) {
        val graphEntry = navController.getBackStackEntry(R.id.select_swap_token_nav_graph)
        val caller = requireNotNull(graphEntry.savedStateHandle.get<Int>(CALLER))
        saveResultTo(navController.getBackStackEntry(caller), response)
        navController.popBackStack(R.id.select_swap_token_nav_graph, true)
    }

    private companion object {
        const val CALLER = "swap.token.caller"
    }
}
