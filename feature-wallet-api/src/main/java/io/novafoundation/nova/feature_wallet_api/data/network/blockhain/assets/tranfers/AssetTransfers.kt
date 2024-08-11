package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_wallet_api.domain.model.OriginDecimalFee
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.ext.accountIdOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope

interface AssetTransfer {
    val sender: MetaAccount
    val recipient: String
    val originChain: Chain
    val originChainAsset: Chain.Asset
    val destinationChain: Chain
    val destinationChainAsset: Chain.Asset
    val commissionAssetToken: Token
    val amount: BigDecimal
}

class BaseAssetTransfer(
    override val sender: MetaAccount,
    override val recipient: String,
    override val originChain: Chain,
    override val originChainAsset: Chain.Asset,
    override val destinationChain: Chain,
    override val destinationChainAsset: Chain.Asset,
    override val commissionAssetToken: Token,
    override val amount: BigDecimal,
) : AssetTransfer

data class WeightedAssetTransfer(
    override val sender: MetaAccount,
    override val recipient: String,
    override val originChain: Chain,
    override val originChainAsset: Chain.Asset,
    override val destinationChain: Chain,
    override val destinationChainAsset: Chain.Asset,
    override val commissionAssetToken: Token,
    override val amount: BigDecimal,
    val decimalFee: OriginDecimalFee,
) : AssetTransfer {

    constructor(assetTransfer: AssetTransfer, fee: OriginDecimalFee) : this(
        sender = assetTransfer.sender,
        recipient = assetTransfer.recipient,
        originChain = assetTransfer.originChain,
        originChainAsset = assetTransfer.originChainAsset,
        destinationChain = assetTransfer.destinationChain,
        destinationChainAsset = assetTransfer.destinationChainAsset,
        commissionAssetToken = assetTransfer.commissionAssetToken,
        amount = assetTransfer.amount,
        decimalFee = fee
    )
}

val AssetTransfer.isCrossChain
    get() = originChain.id != destinationChain.id

fun AssetTransfer.recipientOrNull(): AccountId? {
    return destinationChain.accountIdOrNull(recipient)
}

fun AssetTransfer.senderAccountId(): AccountId {
    return sender.requireAccountIdIn(originChain)
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
