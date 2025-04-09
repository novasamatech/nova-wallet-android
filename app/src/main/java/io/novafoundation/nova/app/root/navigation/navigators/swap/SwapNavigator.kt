package io.novafoundation.nova.app.root.navigation.navigators.swap

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_assets.presentation.balance.detail.BalanceDetailFragment
import io.novafoundation.nova.feature_assets.presentation.send.amount.SendPayload
import io.novafoundation.nova.feature_assets.presentation.swap.asset.AssetSwapFlowFragment
import io.novafoundation.nova.feature_assets.presentation.swap.asset.SwapFlowPayload
import io.novafoundation.nova.feature_assets.presentation.trade.common.TradeProviderFlowType
import io.novafoundation.nova.feature_assets.presentation.trade.provider.TradeProviderListFragment
import io.novafoundation.nova.feature_assets.presentation.trade.provider.TradeProviderListPayload
import io.novafoundation.nova.feature_assets.presentation.trade.webInterface.OnSuccessfulTradeStrategyType
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.main.SwapMainSettingsFragment
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

class SwapNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val commonDelegate: Navigator
) : BaseNavigator(navigationHoldersRegistry), SwapRouter {

    override fun openSwapRoute() {
        navigationBuilder().action(R.id.action_open_swapRouteFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openSwapFee() {
        navigationBuilder().action(R.id.action_open_swapFeeFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openSwapExecution() {
        navigationBuilder().action(R.id.action_swapConfirmationFragment_to_swapExecutionFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openSwapConfirmation() {
        navigationBuilder().action(R.id.action_swapMainSettingsFragment_to_swapConfirmationFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openSwapOptions() {
        navigationBuilder().action(R.id.action_swapMainSettingsFragment_to_swapOptionsFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openRetrySwap(payload: SwapSettingsPayload) {
        navigationBuilder().action(R.id.action_swapExecutionFragment_to_swapSettingsFragment)
            .setArgs(SwapMainSettingsFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openBalanceDetails(assetPayload: AssetPayload) {
        val bundle = BalanceDetailFragment.getBundle(assetPayload)

        navigationBuilder().action(R.id.action_swapExecutionFragment_to_assetDetails)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun openBuyToken(chainId: String, assetId: Int) {
        val bundle = TradeProviderListFragment.createPayload(
            TradeProviderListPayload(
                chainId,
                assetId,
                TradeProviderFlowType.BUY,
                OnSuccessfulTradeStrategyType.RETURN_BACK
            )
        )

        navigationBuilder().action(R.id.action_tradeProvidersFragment)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun selectAssetIn(selectedAsset: AssetPayload?) {
        val payload = SwapFlowPayload.ReselectAssetIn(selectedAsset)
        val bundle = AssetSwapFlowFragment.getBundle(payload)

        navigationBuilder().action(R.id.action_swapSettingsFragment_to_select_swap_token_graph)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun selectAssetOut(selectedAsset: AssetPayload?) {
        val payload = SwapFlowPayload.ReselectAssetOut(selectedAsset)
        val bundle = AssetSwapFlowFragment.getBundle(payload)

        navigationBuilder().action(R.id.action_swapSettingsFragment_to_select_swap_token_graph)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun openSendCrossChain(destination: AssetPayload, recipientAddress: String?) {
        val payload = SendPayload.SpecifiedDestination(destination)

        commonDelegate.openSend(payload, recipientAddress)
    }

    override fun openReceive(assetPayload: AssetPayload) {
        commonDelegate.openReceive(assetPayload)
    }
}
