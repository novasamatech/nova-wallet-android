package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model

import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMetadata
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class NominationPool(
    val id: PoolId,
    val membersCount: Int,
    val status: Status,
    override val metadata: PoolMetadata?,
    override val icon: Icon?,
    override val stashAccountId: AccountId
) : PoolDisplay {

    sealed class Status {

        class Active(val apy: Perbill) : Status()

        object Inactive : Status()
    }
}

val NominationPool.apy: Perbill?
    get() = when (status) {
        is NominationPool.Status.Active -> status.apy
        NominationPool.Status.Inactive -> null
    }
