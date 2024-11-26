package io.novafoundation.nova.app.root.navigation.navigators.swap

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.holders.NavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_assets.presentation.send.amount.SendPayload
import io.novafoundation.nova.feature_assets.presentation.balance.detail.BalanceDetailFragment
import io.novafoundation.nova.feature_assets.presentation.swap.asset.AssetSwapFlowFragment
import io.novafoundation.nova.feature_assets.presentation.swap.asset.SwapFlowPayload
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.SwapConfirmationFragment
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload.SwapConfirmationPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

class SwapNavigator(
    private val navigationHolder: NavigationHolder,
    private val commonDelegate: Navigator
) : BaseNavigator(navigationHolder), SwapRouter {

    override fun openSwapConfirmation(payload: SwapConfirmationPayload) {
        val bundle = SwapConfirmationFragment.getBundle(payload)
        navigationHolder.navController?.navigate(R.id.action_swapMainSettingsFragment_to_swapConfirmationFragment, bundle)
    }

    override fun openSwapOptions() {
        navigationHolder.navController?.navigate(R.id.action_swapMainSettingsFragment_to_swapOptionsFragment)
    }

    override fun openBalanceDetails(assetPayload: AssetPayload) {
        navigationHolder.navController?.navigate(R.id.action_swapConfirmationFragment_to_assetDetails, BalanceDetailFragment.getBundle(assetPayload))
    }

    override fun selectAssetIn(selectedAsset: AssetPayload?) {
        val payload = SwapFlowPayload.ReselectAssetIn(selectedAsset)
        navigationHolder.navController?.navigate(R.id.action_swapSettingsFragment_to_select_swap_token_graph, AssetSwapFlowFragment.getBundle(payload))
    }

    override fun selectAssetOut(selectedAsset: AssetPayload?) {
        val payload = SwapFlowPayload.ReselectAssetOut(selectedAsset)
        navigationHolder.navController?.navigate(R.id.action_swapSettingsFragment_to_select_swap_token_graph, AssetSwapFlowFragment.getBundle(payload))
    }

    override fun openSendCrossChain(destination: AssetPayload, recipientAddress: String?) {
        val payload = SendPayload.SpecifiedDestination(destination)

        commonDelegate.openSend(payload, recipientAddress)
    }

    override fun openReceive(assetPayload: AssetPayload) {
        commonDelegate.openReceive(assetPayload)
    }
}
