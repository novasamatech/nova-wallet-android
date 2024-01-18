package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.utils.mapToSet

typealias ClaimedRewardsPages = Set<Int>

fun bindClaimedPages(decoded: Any?): Set<Int> {
    val asList = decoded.castToList()

    return asList.mapToSet { item ->
        bindNumber(item).toInt()
    }
}
