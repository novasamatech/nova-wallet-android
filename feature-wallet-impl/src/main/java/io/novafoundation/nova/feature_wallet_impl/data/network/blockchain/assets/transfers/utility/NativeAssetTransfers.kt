package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.utility

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.nativeTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.doNotCrossExistentialDeposit
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSourceProvider
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.utility.NativeBalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.notDeadRecipient
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

class NativeAssetTransfers(
    private val nativeBalanceSource: NativeBalanceSource,
    private val balanceSourceProvider: BalanceSourceProvider,
    private val extrinsicService: ExtrinsicService
) : AssetTransfers {

    override val validationSystem: AssetTransfersValidationSystem = ValidationSystem {
        sufficientBalance(
            fee = { it.fee },
            amount = { it.transfer.amount },
            available = { it.usedAsset.transferable },
            error = { AssetTransferValidationFailure.NotEnoughFunds.InUsedAsset }
        )

        notDeadRecipient(
            balanceSourceProvider = balanceSourceProvider,
            assetToCheck = { it.usedAsset },
            addingAmount = { it.transfer.amountInPlanks },
            failure = { AssetTransferValidationFailure.DeadRecipient.InUsedAsset }
        )

        doNotCrossExistentialDeposit(
            totalBalance = { it.usedAsset.total },
            fee = { it.fee },
            extraAmount = { it.transfer.amount },
            existentialDeposit = {
                val inPlanks = nativeBalanceSource.existentialDeposit(it.transfer.chain, it.transfer.chainAsset)

                it.transfer.chainAsset.amountFromPlanks(inPlanks)
            },
            error = { AssetTransferValidationFailure.WillRemoveAccount.WillBurnDust }
        )
    }

    override suspend fun calculateFee(transfer: AssetTransfer): BigInteger {
        return extrinsicService.estimateFee(transfer.chain) {
            nativeTransfer(transfer)
        }
    }

    override suspend fun performTransfer(transfer: AssetTransfer): Result<String> {
        val senderAccountId = transfer.sender.accountIdIn(transfer.chain)!!

        return extrinsicService.submitExtrinsic(transfer.chain, senderAccountId) {
            nativeTransfer(transfer)
        }
    }

    private fun ExtrinsicBuilder.nativeTransfer(transfer: AssetTransfer) {
        nativeTransfer(accountId = transfer.recipient, amount = transfer.chainAsset.planksFromAmount(transfer.amount))
    }
}
