package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

data class AssetTransfer(
    val sender: MetaAccount,
    val recipient: String,
    val originChain: Chain,
    val originChainAsset: Chain.Asset,
    val destinationChain: Chain,
    val destinationChainAsset: Chain.Asset,
    val commissionAssetToken: Token,
    val amount: BigDecimal,
)

val AssetTransfer.isCrossChain
    get() = originChain.id != destinationChain.id

interface AssetTransfers {

    val validationSystem: AssetTransfersValidationSystem

    suspend fun calculateFee(transfer: AssetTransfer): BigInteger

    suspend fun performTransfer(transfer: AssetTransfer): Result<String>

    suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean
}
