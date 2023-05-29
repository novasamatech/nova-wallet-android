package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.ext.accountIdOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigDecimal
import java.math.BigInteger

data class AssetTransfer(
    val sender: MetaAccount,
    val recipient: String,
    val originChain: Chain,
    val originChainAsset: Chain.Asset,
    val destinationChain: Chain,
    val destinationChainAsset: Chain.Asset,
    val amount: BigDecimal,
)

val AssetTransfer.isCrossChain
    get() = originChain.id != destinationChain.id

fun AssetTransfer.recipientOrNull(): AccountId? {
    return destinationChain.accountIdOrNull(recipient)
}

interface AssetTransfers {

    val validationSystem: AssetTransfersValidationSystem

    suspend fun calculateFee(transfer: AssetTransfer): BigInteger

    suspend fun performTransfer(transfer: AssetTransfer): Result<String>

    suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean

    suspend fun recipientCanAcceptTransfer(chainAsset: Chain.Asset, recipient: AccountId): Boolean {
        return true
    }
}
