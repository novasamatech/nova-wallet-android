package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
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
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.doNotCrossExistentialDeposit
import io.novafoundation.nova.feature_wallet_api.domain.validation.enoughTotalToStayAboveED
import io.novafoundation.nova.feature_wallet_api.domain.validation.notPhishingAccount
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.validAddress
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
    private val assetSourceRegistry: AssetSourceRegistry,
    private val extrinsicService: ExtrinsicService,
    private val phishingValidationFactory: PhishingValidationFactory,
) : AssetTransfers {

    protected abstract fun ExtrinsicBuilder.transfer(transfer: AssetTransfer)

    /**
     * Format: [(Module, Function)]
     * Transfers will be enabled if at least one function exists
     */
    protected abstract val transferFunctions: List<Pair<String, String>>

    override suspend fun performTransfer(transfer: AssetTransfer): Result<String> {
        val senderAccountId = transfer.sender.accountIdIn(transfer.originChain)!!

        return extrinsicService.submitExtrinsic(transfer.originChain, senderAccountId) {
            transfer(transfer)
        }
    }

    override suspend fun calculateFee(transfer: AssetTransfer): BigInteger {
        return extrinsicService.estimateFee(transfer.originChain) {
            transfer(transfer)
        }
    }

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        val runtime = chainRegistry.getRuntime(chainAsset.chainId)

        return transferFunctions.any { (module, function) ->
            runtime.metadata.moduleOrNull(module)?.callOrNull(function) != null
        }
    }

    private suspend fun existentialDepositForUsedAsset(transfer: AssetTransfer): BigDecimal {
        return existentialDeposit(transfer.originChain, transfer.originChainAsset)
    }

    private suspend fun existentialDeposit(chain: Chain, asset: Chain.Asset): BigDecimal {
        val inPlanks = assetSourceRegistry.sourceFor(asset).balance
            .existentialDeposit(chain, asset)

        return asset.amountFromPlanks(inPlanks)
    }

    protected fun defaultValidationSystem(
        removeAccountBehavior: ExistentialDepositError<WillRemoveAccount>
    ): AssetTransfersValidationSystem = ValidationSystem {
        validAddress()

        notPhishingRecipient()

        positiveAmount()

        sufficientTransferableBalanceToPayFee()
        sufficientBalanceInUsedAsset()

        sufficientCommissionBalanceToStayAboveED()

        notDeadRecipientInUsedAsset()
        notDeadRecipientInCommissionAsset()

        doNotCrossExistentialDeposit(removeAccountBehavior)
    }

    private fun AssetTransfersValidationSystemBuilder.notPhishingRecipient() = notPhishingAccount(
        factory = phishingValidationFactory,
        address = { it.transfer.recipient },
        chain = { it.transfer.originChain },
        warning = AssetTransferValidationFailure::PhishingRecipient
    )

    private fun AssetTransfersValidationSystemBuilder.validAddress() = validAddress(
        address = { it.transfer.recipient },
        chain = { it.transfer.destinationChain },
        error = { AssetTransferValidationFailure.InvalidRecipientAddress(it.transfer.destinationChain) }
    )

    protected fun AssetTransfersValidationSystemBuilder.positiveAmount() = positiveAmount(
        amount = { it.transfer.amount },
        error = { AssetTransferValidationFailure.NonPositiveAmount }
    )

    protected fun AssetTransfersValidationSystemBuilder.sufficientBalanceInUsedAsset() = sufficientBalance(
        amount = { it.transfer.amount },
        available = { it.usedAsset.transferable },
        error = { AssetTransferValidationFailure.NotEnoughFunds.InUsedAsset },
        fee = { it.feeInUsedAsset }
    )

    protected fun AssetTransfersValidationSystemBuilder.notDeadRecipientInUsedAsset() = notDeadRecipient(
        assetSourceRegistry = assetSourceRegistry,
        assetToCheck = { it.usedAsset },
        addingAmount = { it.transfer.amountInPlanks },
        failure = { AssetTransferValidationFailure.DeadRecipient.InUsedAsset }
    )

    protected fun AssetTransfersValidationSystemBuilder.notDeadRecipientInCommissionAsset() = notDeadRecipient(
        assetSourceRegistry = assetSourceRegistry,
        assetToCheck = { it.commissionAsset },
        addingAmount = { it.amountInCommissionAsset },
        failure = { AssetTransferValidationFailure.DeadRecipient.InCommissionAsset(commissionAsset = it.commissionAsset.token.configuration) }
    )

    protected fun AssetTransfersValidationSystemBuilder.sufficientTransferableBalanceToPayFee() = sufficientBalance(
        fee = { it.originFee },
        available = { it.commissionAsset.transferable },
        error = { AssetTransferValidationFailure.NotEnoughFunds.InCommissionAsset(commissionAsset = it.commissionAsset.token.configuration) }
    )

    protected fun AssetTransfersValidationSystemBuilder.sufficientCommissionBalanceToStayAboveED() = enoughTotalToStayAboveED(
        fee = { it.originFee },
        total = { it.commissionAsset.total },
        existentialDeposit = { existentialDeposit(it.transfer.originChain, it.commissionAsset.token.configuration) },
        error = { AssetTransferValidationFailure.NotEnoughFunds.InCommissionAsset(commissionAsset = it.commissionAsset.token.configuration) }
    )

    protected fun AssetTransfersValidationSystemBuilder.doNotCrossExistentialDeposit(
        error: ExistentialDepositError<WillRemoveAccount>,
    ) = doNotCrossExistentialDeposit(
        totalBalance = { it.usedAsset.total },
        fee = { it.feeInUsedAsset },
        extraAmount = { it.transfer.amount },
        existentialDeposit = { existentialDepositForUsedAsset(it.transfer) },
        error = error
    )
}
