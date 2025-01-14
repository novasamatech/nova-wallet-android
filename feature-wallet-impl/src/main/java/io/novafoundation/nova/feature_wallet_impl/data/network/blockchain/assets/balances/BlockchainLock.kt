package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances

import io.novafoundation.nova.common.data.network.runtime.binding.HelperBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindString
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.second
import io.novafoundation.nova.core_db.dao.LockDao
import io.novafoundation.nova.core_db.model.BalanceLockLocal
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLockId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId



class BlockchainLock(
    val id: BalanceLockId,
    val amount: Balance
)

@HelperBinding
fun bindEquilibriumBalanceLocks(dynamicInstance: Any?): List<BlockchainLock>? {
    if (dynamicInstance == null) return null

    return bindList(dynamicInstance) { items ->
        val item = items.castToList()
        BlockchainLock(
            bindLockIdString(item.first().cast()),
            bindNumber(item.second().cast())
        )
    }
}

@HelperBinding
fun bindBalanceLocks(dynamicInstance: Any?): List<BlockchainLock> {
    if (dynamicInstance == null) return emptyList()

    return bindList(dynamicInstance) {
        BlockchainLock(
            bindLockIdString(it.castToStruct()["id"]),
            bindNumber(it.castToStruct()["amount"])
        )
    }
}

fun bindBalanceFreezes(dynamicInstance: Any?): List<BlockchainLock> {
    if (dynamicInstance == null) return emptyList()

    return bindList(dynamicInstance) { item ->
        val asStruct = item.castToStruct()

        BlockchainLock(
            bindFreezeId(asStruct["id"]),
            bindNumber(asStruct["amount"])
        )
    }
}

private fun bindFreezeId(dynamicInstance: Any?): BalanceLockId {
    val asEnum = dynamicInstance.castToDictEnum()
    val module = asEnum.name
    val moduleReason = asEnum.value.castToDictEnum().name

    return BalanceLockId.fromPath(module, moduleReason)
}

private fun bindLockIdString(dynamicInstance: Any?): BalanceLockId {
    val asString = bindString(dynamicInstance)
    return BalanceLockId.fromFullId(asString)
}


fun mapBlockchainLockToLocal(
    metaId: Long,
    chainId: ChainId,
    assetId: ChainAssetId,
    lock: BlockchainLock
): BalanceLockLocal {
    return BalanceLockLocal(metaId, chainId, assetId, lock.id.value, lock.amount)
}

suspend fun LockDao.updateLocks(locks: List<BlockchainLock>, metaId: Long, chainId: ChainId, chainAssetId: ChainAssetId) {
    val balanceLocksLocal = locks.map { mapBlockchainLockToLocal(metaId, chainId, chainAssetId, it) }
    updateLocks(balanceLocksLocal, metaId, chainId, chainAssetId)
}

suspend fun LockDao.updateLock(lock: BlockchainLock, metaId: Long, chainId: ChainId, chainAssetId: ChainAssetId) {
    val balanceLocksLocal = mapBlockchainLockToLocal(metaId, chainId, chainAssetId, lock)
    updateLocks(listOf(balanceLocksLocal), metaId, chainId, chainAssetId)
}
