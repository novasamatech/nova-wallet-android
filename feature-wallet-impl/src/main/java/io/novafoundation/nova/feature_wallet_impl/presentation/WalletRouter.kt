package io.novafoundation.nova.feature_wallet_impl.presentation

import io.novafoundation.nova.feature_wallet_impl.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferDraft

interface WalletRouter {
    fun openAssetDetails(assetPayload: AssetPayload)

    fun back()

    fun openChooseRecipient(assetPayload: AssetPayload)

    fun openFilter()

    fun openChooseAmount(recipientAddress: String, assetPayload: AssetPayload)

    fun openConfirmTransfer(transferDraft: TransferDraft)

    fun finishSendFlow()

    fun openRepeatTransaction(recipientAddress: String, assetPayload: AssetPayload)

    fun openTransferDetail(transaction: OperationParcelizeModel.Transfer)

    fun openExtrinsicDetail(extrinsic: OperationParcelizeModel.Extrinsic)

    fun openRewardDetail(reward: OperationParcelizeModel.Reward)

    fun openChangeAccountFromWallet()

    fun openReceive(assetPayload: AssetPayload)
}
