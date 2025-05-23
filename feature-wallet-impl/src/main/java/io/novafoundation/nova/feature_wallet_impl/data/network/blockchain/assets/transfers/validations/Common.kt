package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations

import io.novafoundation.nova.feature_account_api.domain.validation.notSystemAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDeposit
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure.WillRemoveAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.commissionChainAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.originFeeList
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.originFeeListInUsedAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.recipientOrNull
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.sendingAmountInCommissionAsset
import io.novafoundation.nova.feature_wallet_api.domain.model.balanceCountedTowardsED
import io.novafoundation.nova.feature_wallet_api.domain.validation.AmountProducer
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.checkForFeeChanges
import io.novafoundation.nova.feature_wallet_api.domain.validation.doNotCrossExistentialDepositMultiFee
import io.novafoundation.nova.feature_wallet_api.domain.validation.notPhishingAccount
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalanceMultiFee
import io.novafoundation.nova.feature_wallet_api.domain.validation.validAddress
import io.novafoundation.nova.feature_wallet_api.domain.validation.validate
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.Type
import kotlinx.coroutines.CoroutineScope
import java.math.BigDecimal

fun AssetTransfersValidationSystemBuilder.positiveAmount() = positiveAmount(
    amount = { it.transfer.amount },
    error = { AssetTransferValidationFailure.NonPositiveAmount }
)

fun AssetTransfersValidationSystemBuilder.notPhishingRecipient(
    factory: PhishingValidationFactory
) = notPhishingAccount(
    factory = factory,
    address = { it.transfer.recipient },
    chain = { it.transfer.destinationChain },
    warning = AssetTransferValidationFailure::PhishingRecipient
)

fun AssetTransfersValidationSystemBuilder.validAddress() = validAddress(
    address = { it.transfer.recipient },
    chain = { it.transfer.destinationChain },
    error = { AssetTransferValidationFailure.InvalidRecipientAddress(it.transfer.destinationChain) }
)

fun AssetTransfersValidationSystemBuilder.sufficientCommissionBalanceToStayAboveED(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) {
    enoughTotalToStayAboveEDValidationFactory.validate(
        fee = { it.originFee.submissionFee },
        balance = { it.originCommissionAsset.balanceCountedTowardsED() },
        chainWithAsset = { ChainWithAsset(it.transfer.originChain, it.commissionChainAsset) },
        error = { payload, error -> AssetTransferValidationFailure.NotEnoughFunds.ToStayAboveED(payload.commissionChainAsset, error) }
    )
}

fun AssetTransfersValidationSystemBuilder.checkForFeeChanges(
    assetSourceRegistry: AssetSourceRegistry,
    coroutineScope: CoroutineScope
) = checkForFeeChanges(
    calculateFee = { payload ->
        val transfers = assetSourceRegistry.sourceFor(payload.transfer.originChainAsset).transfers
        transfers.calculateFee(payload.transfer, coroutineScope)
    },
    currentFee = { it.originFee.submissionFee },
    chainAsset = { it.commissionChainAsset },
    error = AssetTransferValidationFailure::FeeChangeDetected
)

fun AssetTransfersValidationSystemBuilder.doNotCrossExistentialDepositInUsedAsset(
    assetSourceRegistry: AssetSourceRegistry,
    extraAmount: AmountProducer<AssetTransferPayload>,
) = doNotCrossExistentialDepositMultiFee(
    countableTowardsEdBalance = { it.originUsedAsset.balanceCountedTowardsED() },
    fee = { it.originFeeListInUsedAsset },
    extraAmount = extraAmount,
    existentialDeposit = { assetSourceRegistry.existentialDepositForUsedAsset(it.transfer) },
    error = { remainingAmount, payload -> payload.transfer.originChainAsset.existentialDepositError(remainingAmount) }
)

fun AssetTransfersValidationSystemBuilder.sufficientTransferableBalanceToPayOriginFee() = sufficientBalanceMultiFee(
    available = { it.originCommissionAsset.transferable },
    amount = { it.sendingAmountInCommissionAsset },
    feeExtractor = { it.originFeeList },
    error = { context ->
        AssetTransferValidationFailure.NotEnoughFunds.InCommissionAsset(
            chainAsset = context.payload.commissionChainAsset,
            fee = context.fee,
            maxUsable = context.maxUsable
        )
    }
)

fun AssetTransfersValidationSystemBuilder.sufficientBalanceInUsedAsset() = sufficientBalance(
    available = { it.originUsedAsset.transferable },
    amount = { it.transfer.amount },
    fee = { null },
    error = { AssetTransferValidationFailure.NotEnoughFunds.InUsedAsset }
)

fun AssetTransfersValidationSystemBuilder.recipientIsNotSystemAccount() = notSystemAccount(
    accountId = { it.transfer.recipientOrNull() },
    error = { AssetTransferValidationFailure.RecipientIsSystemAccount }
)

private suspend fun AssetSourceRegistry.existentialDepositForUsedAsset(transfer: AssetTransfer): BigDecimal {
    return existentialDeposit(transfer.originChainAsset)
}

private fun Chain.Asset.existentialDepositError(amount: BigDecimal): WillRemoveAccount = when (type) {
    is Type.Native -> WillRemoveAccount.WillBurnDust
    is Type.Orml -> WillRemoveAccount.WillBurnDust
    is Type.Statemine -> WillRemoveAccount.WillTransferDust(amount)
    is Type.EvmErc20, is Type.EvmNative -> WillRemoveAccount.WillBurnDust
    is Type.Equilibrium -> WillRemoveAccount.WillBurnDust
    Type.Unsupported -> throw IllegalArgumentException("Unsupported")
}
