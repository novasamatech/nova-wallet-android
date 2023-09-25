package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.poolState

import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId

fun AccountIdMap<Exposure>.isPoolStaking(poolStash: AccountId, poolNominations: Nominations?): Boolean {
    // whereas pool might still stake without nominations if era it has chilled in haven't yet finished
    // we still mark it as Inactive to warn user preemptively
    if (poolNominations == null) return false

    return poolNominations.targets.any { validator ->
        val accountIdHex = validator.toHexString()
        val exposure = get(accountIdHex) ?: return@any false

        exposure.others.any { it.who.contentEquals(poolStash) }
    }
}
