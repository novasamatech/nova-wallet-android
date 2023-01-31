package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.core_db.model.BalanceLockLocal
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class BalanceLock(
    val id: String,
    val amountInPlanks: Balance,
    val chainAsset: Chain.Asset
): Identifiable {

    override val identifier: String = id
}

fun mapBalanceLockFromLocal(
    asset: Chain.Asset,
    lock: BalanceLockLocal
): BalanceLock {
    return BalanceLock(lock.type, lock.amount, asset)
}

fun List<BalanceLock>.maxLockReplacing(lockId: String, replaceWith: Balance): Balance {
    return maxOfOrNull {
        if (it.id == lockId) replaceWith else it.amountInPlanks
    }.orZero()
}
