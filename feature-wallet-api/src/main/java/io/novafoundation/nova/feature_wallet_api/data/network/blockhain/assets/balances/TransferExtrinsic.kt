package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicStatus
import java.math.BigDecimal
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

data class TransferExtrinsic(
    val senderId: ByteArray,
    val recipientId: ByteArray,
    val amountInPlanks: BigInteger,
    val chainAsset: Chain.Asset,
    val status: ExtrinsicStatus,
    val hash: String,
)

fun List<TransferExtrinsic>.filterOwn(owner: AccountId) = filter {
    it.recipientId.contentEquals(owner) || it.senderId.contentEquals(owner)
}
