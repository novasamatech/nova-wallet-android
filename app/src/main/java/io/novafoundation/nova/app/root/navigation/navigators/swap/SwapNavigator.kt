package io.novafoundation.nova.app.root.navigation.navigators.swap

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
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
    private val splitScreenNavigationHolder: SplitScreenNavigationHolder,
    private val rootNavigationHolder: RootNavigationHolder,
    private val commonDelegate: Navigator
) : BaseNavigator(splitScreenNavigationHolder, rootNavigationHolder), SwapRouter {

    override fun openSwapConfirmation(payload: SwapConfirmationPayload) {
        val bundle = SwapConfirmationFragment.getBundle(payload)

        navigationBuilder(R.id.action_swapMainSettingsFragment_to_swapConfirmationFragment)
            .setArgs(bundle)
            .perform()
    }

    override fun openSwapOptions() {
        navigationBuilder(R.id.action_swapMainSettingsFragment_to_swapOptionsFragment)
            .perform()
    }

    override fun openBalanceDetails(assetPayload: AssetPayload) {
        val bundle = BalanceDetailFragment.getBundle(assetPayload)

        navigationBuilder(R.id.action_swapConfirmationFragment_to_assetDetails)
            .setArgs(bundle)
            .perform()
    }

    override fun selectAssetIn(selectedAsset: AssetPayload?) {
        val payload = SwapFlowPayload.ReselectAssetIn(selectedAsset)
        val bundle = AssetSwapFlowFragment.getBundle(payload)

        navigationBuilder(R.id.action_swapSettingsFragment_to_select_swap_token_graph)
            .setArgs(bundle)
            .perform()
    }

    override fun selectAssetOut(selectedAsset: AssetPayload?) {
        val payload = SwapFlowPayload.ReselectAssetOut(selectedAsset)
        val bundle = AssetSwapFlowFragment.getBundle(payload)

        navigationBuilder(R.id.action_swapSettingsFragment_to_select_swap_token_graph)
            .setArgs(bundle)
            .perform()
    }

    override fun openSendCrossChain(destination: AssetPayload, recipientAddress: String?) {
        val payload = SendPayload.SpecifiedDestination(destination)

        commonDelegate.openSend(payload, recipientAddress)
    }

    override fun openReceive(assetPayload: AssetPayload) {
        commonDelegate.openReceive(assetPayload)
    }
}
