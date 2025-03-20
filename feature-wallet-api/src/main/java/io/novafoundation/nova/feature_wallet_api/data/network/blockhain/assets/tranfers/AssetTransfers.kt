package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers

import io.novafoundation.nova.common.utils.amountFromPlanks
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.OriginFee
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.accountIdOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.math.BigDecimal

interface AssetTransferDirection {

    val originChain: Chain

    val originChainAsset: Chain.Asset

    val destinationChain: Chain

    val destinationChainAsset: Chain.Asset
}

interface AssetTransferBase : AssetTransferDirection {

    val recipientAccountId: AccountId
        get() = destinationChain.accountIdOf(recipient)

    val recipient: String

    val feePaymentCurrency: FeePaymentCurrency

    val amountPlanks: Balance
}

fun AssetTransferBase.amount(): BigDecimal {
    return originChainAsset.amountFromPlanks(amountPlanks)
}

fun AssetTransferBase.replaceAmount(newAmount: Balance): AssetTransferBase {
    return AssetTransferBase(recipient, originChain, originChainAsset, destinationChain, destinationChainAsset, feePaymentCurrency, newAmount)
}

// TODO this is too specialized for this module
interface AssetTransfer : AssetTransferBase {

    val sender: MetaAccount

    val amount: BigDecimal

    val transferringMaxAmount: Boolean

    override val amountPlanks: Balance
        get() = originChainAsset.planksFromAmount(amount)
}

fun AssetTransferDirection(
    originChain: Chain,
    originChainAsset: Chain.Asset,
    destinationChain: Chain,
    destinationChainAsset: Chain.Asset
): AssetTransferDirection {
    return object : AssetTransferDirection {
        override val originChain: Chain = originChain
        override val originChainAsset: Chain.Asset = originChainAsset
        override val destinationChain: Chain = destinationChain
        override val destinationChainAsset: Chain.Asset = destinationChainAsset
    }
}

fun AssetTransferBase(
    recipient: String,
    originChain: Chain,
    originChainAsset: Chain.Asset,
    destinationChain: Chain,
    destinationChainAsset: Chain.Asset,
    feePaymentCurrency: FeePaymentCurrency,
    amountPlanks: Balance
): AssetTransferBase {
    return object : AssetTransferBase {
        override val recipient: String = recipient
        override val originChain: Chain = originChain
        override val originChainAsset: Chain.Asset = originChainAsset
        override val destinationChain: Chain = destinationChain
        override val destinationChainAsset: Chain.Asset = destinationChainAsset
        override val feePaymentCurrency: FeePaymentCurrency = feePaymentCurrency
        override val amountPlanks: Balance = amountPlanks
    }
}

class BaseAssetTransfer(
    override val sender: MetaAccount,
    override val recipient: String,
    override val originChain: Chain,
    override val originChainAsset: Chain.Asset,
    override val destinationChain: Chain,
    override val destinationChainAsset: Chain.Asset,
    override val feePaymentCurrency: FeePaymentCurrency,
    override val amount: BigDecimal,
    override val transferringMaxAmount: Boolean
) : AssetTransfer

data class WeightedAssetTransfer(
    override val sender: MetaAccount,
    override val recipient: String,
    override val originChain: Chain,
    override val originChainAsset: Chain.Asset,
    override val destinationChain: Chain,
    override val destinationChainAsset: Chain.Asset,
    override val feePaymentCurrency: FeePaymentCurrency,
    override val amount: BigDecimal,
    override val transferringMaxAmount: Boolean,
    val fee: OriginFee,
) : AssetTransfer {

    constructor(assetTransfer: AssetTransfer, fee: OriginFee) : this(
        sender = assetTransfer.sender,
        recipient = assetTransfer.recipient,
        originChain = assetTransfer.originChain,
        originChainAsset = assetTransfer.originChainAsset,
        destinationChain = assetTransfer.destinationChain,
        destinationChainAsset = assetTransfer.destinationChainAsset,
        feePaymentCurrency = assetTransfer.feePaymentCurrency,
        amount = assetTransfer.amount,
        transferringMaxAmount = assetTransfer.transferringMaxAmount,
        fee = fee
    )
}

val AssetTransfer.isCrossChain
    get() = originChain.id != destinationChain.id

fun AssetTransfer.recipientOrNull(): AccountId? {
    return destinationChain.accountIdOrNull(recipient)
}

interface AssetTransfers {

    fun getValidationSystem(coroutineScope: CoroutineScope): AssetTransfersValidationSystem

    suspend fun calculateFee(transfer: AssetTransfer, coroutineScope: CoroutineScope): Fee

    suspend fun performTransfer(transfer: WeightedAssetTransfer, coroutineScope: CoroutineScope): Result<ExtrinsicSubmission>

    suspend fun totalCanDropBelowMinimumBalance(chainAsset: Chain.Asset): Boolean {
        return true
    }

    fun totalCanDropBelowMinimumBalanceFlow(chainAsset: Chain.Asset): Flow<Boolean> {
        return flowOf(true)
    }

    suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean

    suspend fun recipientCanAcceptTransfer(chainAsset: Chain.Asset, recipient: AccountId): Boolean {
        return true
    }
}
