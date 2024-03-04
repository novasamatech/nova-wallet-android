package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.poolState

import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId

fun AccountIdMap<Exposure>.isPoolStaking(poolStash: AccountId, poolNominations: Nominations?): Boolean {
    // whereas pool might still stake without nominations if era it has chilled in haven't yet finished
    // we still mark it as Inactive to warn user preemptively
    if (poolNominations == null) return false

    // fast path - we don't need to traverse the entire exposure map if pool is staking with some of its current validators
    if (isPoolStakingWithCurrentNominations(poolStash, poolNominations)) return true

    // slow path - despite pool is not staking with its current nomination it might still stake with a validator if it changed nominations recently
    return isPoolStakingWithAnyValidator(poolStash)
}

private fun AccountIdMap<Exposure>.isPoolStakingWithCurrentNominations(poolStash: AccountId, poolNominations: Nominations): Boolean {
    return poolNominations.targets.any { validator ->
        val accountIdHex = validator.toHexString()
        val exposure = get(accountIdHex) ?: return@any false

        exposure.others.any { it.who.contentEquals(poolStash) }
    }
}

private fun AccountIdMap<Exposure>.isPoolStakingWithAnyValidator(poolStash: AccountId): Boolean {
    return any { (_, exposure) ->
        exposure.others.any { it.who.contentEquals(poolStash) }
    }
}
