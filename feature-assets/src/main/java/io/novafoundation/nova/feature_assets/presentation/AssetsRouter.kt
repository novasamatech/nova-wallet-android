package io.novafoundation.nova.feature_assets.presentation

import android.os.Bundle
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft
import io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo.AddTokenEnterInfoPayload
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.ManageChainTokensPayload
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.TransactionHistoryFilterPayload

interface AssetsRouter {

    fun openAssetDetails(assetPayload: AssetPayload)

    fun back()

    fun openFilter(payload: TransactionHistoryFilterPayload)

    fun openSend(assetPayload: AssetPayload, initialRecipientAddress: String? = null)

    fun openConfirmTransfer(transferDraft: TransferDraft)

    fun openTransferDetail(transaction: OperationParcelizeModel.Transfer)

    fun openExtrinsicDetail(extrinsic: OperationParcelizeModel.Extrinsic)

    fun openRewardDetail(reward: OperationParcelizeModel.Reward)

    fun openPoolRewardDetail(reward: OperationParcelizeModel.PoolReward)

    fun openSwitchWallet()

    fun openSelectAddress(arguments: Bundle)

    fun openReceive(assetPayload: AssetPayload)

    fun openAssetFilters()

    fun openAssetSearch()

    fun openManageTokens()

    fun openManageChainTokens(payload: ManageChainTokensPayload)

    fun openAddTokenEnterInfo(payload: AddTokenEnterInfoPayload)

    fun openAddTokenSelectChain()

    fun openSendFlow()

    fun openReceiveFlow()

    fun openBuyFlow()

    fun openNfts()

    fun finishAddTokenFlow()

    fun openWalletConnectSessions(metaId: Long)

    fun openWalletConnectScan()
}
