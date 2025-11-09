package io.novafoundation.nova.feature_gift_impl.domain.models

import io.novafoundation.nova.feature_gift_impl.domain.models.Gift.Status.PENDING
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger
import java.util.Date

class Gift(
    val id: Long,
    val amount: BigInteger,
    val creatorMetaId: Long,
    val chainId: ChainId,
    val assetId: ChainAssetId,
    val status: Status,
    val giftAccountId: ByteArray,
    val creationDate: Date
) {
    enum class Status {
        PENDING,
        CLAIMED,
        RECLAIMED
    }
}

fun Gift.Status.isClaimed() = this != PENDING
