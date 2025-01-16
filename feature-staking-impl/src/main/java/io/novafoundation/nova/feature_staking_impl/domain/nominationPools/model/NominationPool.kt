package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMetadata
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolState
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

class NominationPool(
    val id: PoolId,
    val membersCount: Int,
    val state: PoolState,
    val status: Status,
    override val metadata: PoolMetadata?,
    override val icon: Icon?,
    override val stashAccountId: AccountId
) : PoolDisplay {

    sealed class Status {

        class Active(val apy: Fraction) : Status()

        object Inactive : Status()
    }
}

val NominationPool.apy: Fraction?
    get() = when (status) {
        is NominationPool.Status.Active -> status.apy
        NominationPool.Status.Inactive -> null
    }

val NominationPool.Status.isActive: Boolean
    get() = this is NominationPool.Status.Active

fun NominationPool.address(chain: Chain): String {
    return chain.addressOf(stashAccountId)
}

fun NominationPool.name(): String? {
    return metadata?.title
}

fun NominationPool.nameOrAddress(chain: Chain): String {
    return name() ?: address(chain)
}
