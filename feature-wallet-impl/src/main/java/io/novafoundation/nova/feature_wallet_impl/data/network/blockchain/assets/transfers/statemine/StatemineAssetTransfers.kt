package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.statemine

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.validation.doNotCrossExistentialDeposit
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSourceProvider
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine.StatemineBalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.notDeadRecipient
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

class StatemineAssetTransfers(
    private val statemineBalanceSource: StatemineBalanceSource,
    private val balanceSourceProvider: BalanceSourceProvider,
    private val extrinsicService: ExtrinsicService
) : AssetTransfers {

    override val validationSystem: AssetTransfersValidationSystem = ValidationSystem {
        // sender is able to pay fees
        sufficientBalance(
            fee = { it.fee },
            available = { it.commissionAsset.transferable },
            error = { AssetTransferValidationFailure.NotEnoughFunds.InCommissionAsset(commissionAsset = it.commissionAsset.token.configuration) }
        )

        // sender has enough balance of used asset to transfer
        // also handles situation when balance is frozen
        sufficientBalance(
            amount = { it.transfer.amount },
            available = { it.usedAsset.transferable },
            error = { AssetTransferValidationFailure.NotEnoughFunds.InUsedAsset }
        )

        // recipient has balance > ED in transferring token
        notDeadRecipient(
            balanceSourceProvider = balanceSourceProvider,
            assetToCheck = { it.usedAsset },
            addingAmount = { it.transfer.amountInPlanks },
            failure = { AssetTransferValidationFailure.DeadRecipient.InUsedAsset }
        )

        // recipient has balance > ED in commission token
        notDeadRecipient(
            balanceSourceProvider = balanceSourceProvider,
            assetToCheck = { it.commissionAsset },
            failure = { AssetTransferValidationFailure.DeadRecipient.InCommissionAsset(commissionAsset = it.commissionAsset.token.configuration) }
        )

        // sender wont cross ED
        doNotCrossExistentialDeposit(
            totalBalance = { it.usedAsset.total },
            extraAmount = { it.transfer.amount },
            existentialDeposit = {
                val inPlanks = statemineBalanceSource.existentialDeposit(it.transfer.chain, it.transfer.chainAsset)

                it.transfer.chainAsset.amountFromPlanks(inPlanks)
            },
            error = AssetTransferValidationFailure.WillRemoveAccount::WillTransferDust
        )
    }

    override suspend fun calculateFee(transfer: AssetTransfer): BigInteger {
        return extrinsicService.estimateFee(transfer.chain) {
            statemineTransfer(transfer)
        }
    }

    override suspend fun performTransfer(transfer: AssetTransfer): Result<String> {
        val senderAccountId = transfer.sender.accountIdIn(transfer.chain)!!

        return extrinsicService.submitExtrinsic(transfer.chain, senderAccountId) {
            statemineTransfer(transfer)
        }
    }

    private fun ExtrinsicBuilder.statemineTransfer(transfer: AssetTransfer) {
        val chainAssetType = transfer.chainAsset.type
        require(chainAssetType is Chain.Asset.Type.Statemine)

        statemineTransfer(
            assetId = chainAssetType.id,
            target = transfer.recipient,
            amount = transfer.amountInPlanks
        )
    }

    private fun ExtrinsicBuilder.statemineTransfer(
        assetId: BigInteger,
        target: AccountId,
        amount: BigInteger
    ) {
        call(
            moduleName = Modules.ASSETS,
            callName = "transfer",
            arguments = mapOf(
                "id" to assetId,
                "target" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, target),
                "amount" to amount
            )
        )
    }
}
