package io.novafoundation.nova.feature_assets.presentation

import android.os.Bundle
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft
import io.novafoundation.nova.feature_assets.presentation.send.amount.SendPayload
import io.novafoundation.nova.feature_assets.presentation.swap.network.NetworkSwapFlowPayload
import io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo.AddTokenEnterInfoPayload
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.ManageChainTokensPayload
import io.novafoundation.nova.feature_assets.presentation.trade.webInterface.TradeWebPayload
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.TransactionHistoryFilterPayload
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

interface AssetsRouter {

    fun openAssetDetails(assetPayload: AssetPayload)

    fun finishTradeOperation()

    fun back()

    fun openFilter(payload: TransactionHistoryFilterPayload)

    fun openSend(payload: SendPayload, initialRecipientAddress: String? = null)

    fun openConfirmTransfer(transferDraft: TransferDraft)

    fun openTransferDetail(transaction: OperationParcelizeModel.Transfer)

    fun openExtrinsicDetail(extrinsic: OperationParcelizeModel.Extrinsic)

    fun openRewardDetail(reward: OperationParcelizeModel.Reward)

    fun openPoolRewardDetail(reward: OperationParcelizeModel.PoolReward)

    fun openSwapDetail(swap: OperationParcelizeModel.Swap)

    fun openSwitchWallet()

    fun openSelectAddress(arguments: Bundle)

    fun openSelectMultipleWallets(arguments: Bundle)

    fun openReceive(assetPayload: AssetPayload)

    fun openAssetSearch()

    fun openManageTokens()

    fun openManageChainTokens(payload: ManageChainTokensPayload)

    fun openAddTokenEnterInfo(payload: AddTokenEnterInfoPayload)

    fun openAddTokenSelectChain()

    fun openSendFlow()

    fun openReceiveFlow()

    fun openBuyFlow()

    fun openSellFlow()

    fun openBuyFlowFromSendFlow()

    fun openNfts()

    fun finishAddTokenFlow()

    fun openWalletConnectSessions(metaId: Long)

    fun openWalletConnectScan()

    fun openSwapFlow()

    fun openSwapSetupAmount(swapSettingsPayload: SwapSettingsPayload)

    fun returnToMainScreen()

    fun finishSelectAndOpenSwapSetupAmount(swapSettingsPayload: SwapSettingsPayload)

    fun closeSendFlow()

    fun openNovaCard()

    fun openAwaitingCardCreation()

    fun closeNovaCard()

    fun openSendNetworks(payload: NetworkFlowPayload)

    fun openReceiveNetworks(payload: NetworkFlowPayload)

    fun openSwapNetworks(payload: NetworkSwapFlowPayload)

    fun returnToMainSwapScreen()

    fun openBuyNetworks(payload: NetworkFlowPayload)

    fun openSellNetworks(payload: NetworkFlowPayload)

    fun openGiftsNetworks(payload: NetworkFlowPayload)

    fun openBuyProviders(chainId: String, chainAssetId: Int)

    fun openSellProviders(chainId: String, chainAssetId: Int)

    fun openTradeWebInterface(payload: TradeWebPayload)

    fun finishTopUp()

    fun openPendingMultisigOperations()

    fun openAssetDetailsFromDeepLink(payload: AssetPayload)

    fun openGifts()

    fun openGiftsByAsset(assetPayload: AssetPayload)
    fun openSelectGiftAmount(assetPayload: AssetPayload)
}
