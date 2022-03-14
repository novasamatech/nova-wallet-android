package io.novafoundation.nova.feature_assets.presentation

import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft

interface WalletRouter {

    fun openAssetDetails(assetPayload: AssetPayload)

    fun back()

    fun openFilter()

    fun openSend(assetPayload: AssetPayload, initialRecipientAddress: String? = null)

    fun openConfirmTransfer(transferDraft: TransferDraft)

    fun finishSendFlow()

    fun openTransferDetail(transaction: OperationParcelizeModel.Transfer)

    fun openExtrinsicDetail(extrinsic: OperationParcelizeModel.Extrinsic)

    fun openRewardDetail(reward: OperationParcelizeModel.Reward)

    fun openChangeAccount()

    fun openReceive(assetPayload: AssetPayload)

    fun openAssetFilters()

    fun openNfts()
}
