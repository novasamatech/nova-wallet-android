package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId

fun bindPoolId(decoded: Any?): PoolId {
    return PoolId(bindNumber(decoded))
}
