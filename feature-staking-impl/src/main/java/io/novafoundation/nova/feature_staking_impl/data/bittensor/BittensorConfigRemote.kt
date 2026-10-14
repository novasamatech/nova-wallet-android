package io.novafoundation.nova.feature_staking_impl.data.bittensor

import java.math.BigDecimal

class BittensorConfigRemote(
    val swapFee: BigDecimal?,
    val subnets: List<Subnet?>?
) {

    class Subnet(val netuid: Int?, val logo: String?)
}
