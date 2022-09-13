package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.core_db.model.BalanceLockLocal
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class BalanceLock(
    val id: String,
    val amountInPlanks: Balance,
    val chainAsset: Chain.Asset
)

fun mapBalanceLockFromLocal(
    asset: Chain.Asset,
    lock: BalanceLockLocal
): BalanceLock {
    return BalanceLock(lock.type, lock.amount, asset)
}
