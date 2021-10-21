package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.HelperBinding
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.feature_staking_api.domain.model.BlockNumber

@HelperBinding
fun bindBlockNumber(dynamicInstance: Any?): BlockNumber {
    return dynamicInstance.cast()
}
