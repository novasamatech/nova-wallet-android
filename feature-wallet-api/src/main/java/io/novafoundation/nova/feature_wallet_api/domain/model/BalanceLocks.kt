package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.core_db.model.BalanceLockLocal
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class BalanceLock(
    val id: BalanceLockId,
    val amountInPlanks: Balance,
    val chainAsset: Chain.Asset
) : Identifiable {

    override val identifier: String = id.value
}

@JvmInline
value class BalanceLockId private constructor(val value: String) : Identifiable {

    override val identifier: String
        get() = value

    companion object {

        fun fromPath(vararg pathSegments: String): BalanceLockId {
            val fullId = pathSegments.joinToString(separator = ": ")
            return fromFullId(fullId)
        }

        fun fromFullId(fullId: String): BalanceLockId {
            return BalanceLockId(fullId)
        }
    }
}

fun mapBalanceLockFromLocal(
    asset: Chain.Asset,
    lock: BalanceLockLocal
): BalanceLock {
    return BalanceLock(
        id = BalanceLockId.fromFullId(lock.type),
        amountInPlanks = lock.amount,
        chainAsset = asset
    )
}

fun List<BalanceLock>.maxLockReplacing(lockId: BalanceLockId, replaceWith: Balance): Balance {
    return maxOfOrNull {
        if (it.id == lockId) replaceWith else it.amountInPlanks
    }.orZero()
}
