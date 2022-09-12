package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances

import io.novafoundation.nova.common.data.network.runtime.binding.HelperBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindString
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.core_db.model.BalanceLockLocal
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class BlockchainLock(
    val id: String,
    val amount: Balance
)

@HelperBinding
fun bindBalanceLocks(dynamicInstance: Any?): List<BlockchainLock>? {
    if (dynamicInstance == null) return null

    return bindList(dynamicInstance) {
        BlockchainLock(
            bindString(it.castToStruct()["id"]),
            bindNumber(it.castToStruct()["amount"])
        )
    }
}

fun mapBlockchainLockToLocal(
    metaId: Long,
    chainId: ChainId,
    assetId: ChainAssetId,
    lock: BlockchainLock
): BalanceLockLocal {
    return BalanceLockLocal(metaId, chainId, assetId, lock.id, lock.amount)
}
