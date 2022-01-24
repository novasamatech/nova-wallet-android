package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure.WillRemoveAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInCommissionAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.feeInUsedAsset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.validation.ExistentialDepositError
import io.novafoundation.nova.feature_wallet_api.domain.validation.doNotCrossExistentialDeposit
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSourceProvider
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.notDeadRecipient
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import jp.co.soramitsu.fearless_utils.runtime.metadata.callOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.moduleOrNull
import java.math.BigDecimal
import java.math.BigInteger

typealias AssetTransfersValidationSystemBuilder = ValidationSystemBuilder<AssetTransferPayload, AssetTransferValidationFailure>

abstract class BaseAssetTransfers(
    private val chainRegistry: ChainRegistry,
    private val balanceSourceProvider: BalanceSourceProvider,
    private val extrinsicService: ExtrinsicService,
) : AssetTransfers {

    protected abstract fun ExtrinsicBuilder.transfer(transfer: AssetTransfer)

    /**
     * (Module, Function)
     */
    protected abstract val transferFunction: Pair<String, String>

    override suspend fun performTransfer(transfer: AssetTransfer): Result<String> {
        val senderAccountId = transfer.sender.accountIdIn(transfer.chain)!!

        return extrinsicService.submitExtrinsic(transfer.chain, senderAccountId) {
            transfer(transfer)
        }
    }

    override suspend fun calculateFee(transfer: AssetTransfer): BigInteger {
        return extrinsicService.estimateFee(transfer.chain) {
            transfer(transfer)
        }
    }

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        val (module, function) = transferFunction

        val runtime = chainRegistry.getRuntime(chainAsset.chainId)

        return runtime.metadata.moduleOrNull(module)?.callOrNull(function) != null
    }

    private suspend fun existentialDepositFor(transfer: AssetTransfer): BigDecimal {
        val inPlanks = balanceSourceProvider.provideFor(transfer.chainAsset)
            .existentialDeposit(transfer.chain, transfer.chainAsset)

        return transfer.chainAsset.amountFromPlanks(inPlanks)
    }

    protected fun defaultValidationSystem(
        removeAccountBehavior: ExistentialDepositError<WillRemoveAccount>
    ): AssetTransfersValidationSystem = ValidationSystem {
        sufficientBalanceForCommission()
        sufficientBalanceInUsedAsset()

        notDeadRecipientInUsedAsset()
        notDeadRecipientInCommissionAsset()

        doNotCrossExistentialDeposit(removeAccountBehavior)
    }

    protected fun AssetTransfersValidationSystemBuilder.sufficientBalanceInUsedAsset() = sufficientBalance(
        amount = { it.transfer.amount },
        available = { it.usedAsset.transferable },
        error = { AssetTransferValidationFailure.NotEnoughFunds.InUsedAsset },
        fee = { it.feeInUsedAsset }
    )

    protected fun AssetTransfersValidationSystemBuilder.notDeadRecipientInUsedAsset() = notDeadRecipient(
        balanceSourceProvider = balanceSourceProvider,
        assetToCheck = { it.usedAsset },
        addingAmount = { it.transfer.amountInPlanks },
        failure = { AssetTransferValidationFailure.DeadRecipient.InUsedAsset }
    )

    protected fun AssetTransfersValidationSystemBuilder.notDeadRecipientInCommissionAsset() = notDeadRecipient(
        balanceSourceProvider = balanceSourceProvider,
        assetToCheck = { it.commissionAsset },
        addingAmount = { it.amountInCommissionAsset },
        failure = { AssetTransferValidationFailure.DeadRecipient.InCommissionAsset(commissionAsset = it.commissionAsset.token.configuration) }
    )

    protected fun AssetTransfersValidationSystemBuilder.sufficientBalanceForCommission() = sufficientBalance(
        fee = { it.fee },
        available = { it.commissionAsset.transferable },
        error = { AssetTransferValidationFailure.NotEnoughFunds.InCommissionAsset(commissionAsset = it.commissionAsset.token.configuration) }
    )

    protected fun AssetTransfersValidationSystemBuilder.doNotCrossExistentialDeposit(
        error: ExistentialDepositError<WillRemoveAccount>,
    ) = doNotCrossExistentialDeposit(
        totalBalance = { it.usedAsset.total },
        fee = { it.feeInUsedAsset },
        extraAmount = { it.transfer.amount },
        existentialDeposit = { existentialDepositFor(it.transfer) },
        error = error
    )
}
