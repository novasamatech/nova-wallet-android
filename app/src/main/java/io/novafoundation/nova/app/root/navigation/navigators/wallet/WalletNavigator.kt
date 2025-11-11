package io.novafoundation.nova.app.root.navigation.navigators.wallet

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_assets.presentation.send.amount.SendPayload
import io.novafoundation.nova.feature_assets.presentation.trade.common.TradeProviderFlowType
import io.novafoundation.nova.feature_assets.presentation.trade.provider.TradeProviderListFragment
import io.novafoundation.nova.feature_assets.presentation.trade.provider.TradeProviderListPayload
import io.novafoundation.nova.feature_assets.presentation.trade.webInterface.OnSuccessfulTradeStrategyType
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter

class WalletNavigator(
    private val commonDelegate: Navigator,
    navigationHoldersRegistry: NavigationHoldersRegistry
) : BaseNavigator(navigationHoldersRegistry), WalletRouter {

    override fun openSendCrossChain(destination: AssetPayload, recipientAddress: String?) {
        val payload = SendPayload.SpecifiedDestination(destination)

        commonDelegate.openSend(payload, recipientAddress)
    }

    override fun openReceive(assetPayload: AssetPayload) {
        commonDelegate.openReceive(assetPayload)
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
}
