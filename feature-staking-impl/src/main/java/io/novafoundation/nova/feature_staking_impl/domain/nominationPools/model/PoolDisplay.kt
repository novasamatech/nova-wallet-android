package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMetadata
import io.novasama.substrate_sdk_android.runtime.AccountId

interface PoolDisplay {

    val icon: Icon?

    val metadata: PoolMetadata?

    val stashAccountId: AccountId
}
