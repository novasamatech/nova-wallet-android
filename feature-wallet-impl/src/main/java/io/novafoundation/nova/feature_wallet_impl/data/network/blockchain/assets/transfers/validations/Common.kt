package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations

import io.novafoundation.nova.feature_account_api.domain.validation.notSystemAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure.WillRemoveAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.recipientOrNull
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.sendingAmountInCommissionAsset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.validation.AmountProducer
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.checkForFeeChanges
import io.novafoundation.nova.feature_wallet_api.domain.validation.doNotCrossExistentialDeposit
import io.novafoundation.nova.feature_wallet_api.domain.validation.enoughTotalToStayAboveED
import io.novafoundation.nova.feature_wallet_api.domain.validation.notPhishingAccount
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.validAddress
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.Type
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
    assetSourceRegistry: AssetSourceRegistry
) = enoughTotalToStayAboveED(
    fee = { it.originFee },
    total = { it.originCommissionAsset.total },
    existentialDeposit = { assetSourceRegistry.existentialDeposit(it.transfer.originChain, it.transfer.originChain.commissionAsset) },
    error = { AssetTransferValidationFailure.NotEnoughFunds.ToStayAboveED(it.transfer.originChain.commissionAsset) }
)

fun AssetTransfersValidationSystemBuilder.checkForFeeChanges(
    assetSourceRegistry: AssetSourceRegistry
) = checkForFeeChanges(
    calculateFee = {
        val transfers = assetSourceRegistry.sourceFor(it.transfer.originChainAsset).transfers
        transfers.calculateFee(it.transfer)
    },
    currentFee = { it.originFee },
    chainAsset = { it.transfer.commissionAssetToken.configuration },
    error = AssetTransferValidationFailure::FeeChangeDetected
)

fun AssetTransfersValidationSystemBuilder.doNotCrossExistentialDeposit(
    assetSourceRegistry: AssetSourceRegistry,
    fee: AmountProducer<AssetTransferPayload>,
    extraAmount: AmountProducer<AssetTransferPayload>,
) = doNotCrossExistentialDeposit(
    totalBalance = { it.originUsedAsset.total },
    fee = fee,
    extraAmount = extraAmount,
    existentialDeposit = { assetSourceRegistry.existentialDepositForUsedAsset(it.transfer) },
    error = { remainingAmount, payload -> payload.transfer.originChainAsset.existentialDepositError(remainingAmount) }
)

fun AssetTransfersValidationSystemBuilder.sufficientTransferableBalanceToPayOriginFee() = sufficientBalance(
    available = { it.originCommissionAsset.transferable },
    amount = { it.sendingAmountInCommissionAsset },
    fee = { it.originFee },
    error = { payload, availableToPayFees ->
        AssetTransferValidationFailure.NotEnoughFunds.InCommissionAsset(
            chainAsset = payload.transfer.originChain.commissionAsset,
            fee = payload.originFee,
            availableToPayFees = availableToPayFees
        )
    }
)

fun AssetTransfersValidationSystemBuilder.sufficientBalanceInUsedAsset() = sufficientBalance(
    available = { it.originUsedAsset.transferable },
    amount = { it.transfer.amount },
    fee = { BigDecimal.ZERO },
    error = { _, _ -> AssetTransferValidationFailure.NotEnoughFunds.InUsedAsset }
)

fun AssetTransfersValidationSystemBuilder.recipientIsNotSystemAccount() = notSystemAccount(
    accountId = { it.transfer.recipientOrNull() },
    error = { AssetTransferValidationFailure.RecipientIsSystemAccount }
)

private suspend fun AssetSourceRegistry.existentialDepositForUsedAsset(transfer: AssetTransfer): BigDecimal {
    return existentialDeposit(transfer.originChain, transfer.originChainAsset)
}

private suspend fun AssetSourceRegistry.existentialDeposit(chain: Chain, asset: Chain.Asset): BigDecimal {
    val inPlanks = sourceFor(asset).balance.existentialDeposit(chain, asset)

    return asset.amountFromPlanks(inPlanks)
}

private fun Chain.Asset.existentialDepositError(amount: BigDecimal): WillRemoveAccount = when (type) {
    is Type.Native -> WillRemoveAccount.WillBurnDust
    is Type.Orml -> WillRemoveAccount.WillBurnDust
    is Type.Statemine -> WillRemoveAccount.WillTransferDust(amount)
    is Type.EvmErc20, is Type.EvmNative -> WillRemoveAccount.WillBurnDust
    is Type.Equilibrium -> WillRemoveAccount.WillBurnDust
    Type.Unsupported -> throw IllegalArgumentException("Unsupported")
}
