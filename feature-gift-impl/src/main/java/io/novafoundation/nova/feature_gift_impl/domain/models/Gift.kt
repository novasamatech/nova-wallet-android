package io.novafoundation.nova.feature_gift_impl.domain.models

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger

class Gift(
    val id: Int,
    val amount: BigInteger,
    val chainId: ChainId,
    val assetId: ChainAssetId,
    val status: Status
) {
    enum class Status {
        PENDING,
        CLAIMED,
        RECLAIMED
    }
}
