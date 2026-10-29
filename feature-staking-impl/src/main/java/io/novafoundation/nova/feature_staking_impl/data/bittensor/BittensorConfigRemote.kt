package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.annotations.JsonAdapter

class BittensorConfigRemote(
    @JsonAdapter(BittensorSwapFeeAdapter::class)
    val swapFee: BittensorSwapFeeRemote?,
    val subnets: List<Subnet?>?
) {

    class Subnet(val netuid: Int?, val logo: String?)
}
