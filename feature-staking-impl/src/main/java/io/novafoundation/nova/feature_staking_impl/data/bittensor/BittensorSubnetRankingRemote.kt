package io.novafoundation.nova.feature_staking_impl.data.bittensor

import java.math.BigInteger

class BittensorSubnetRankingRemote(
    val netuid: Int?,
    val taoIn: String?,
    val ageBlocks: BigInteger?,
    val breakdown: Breakdown?
) {

    class Breakdown(val volatility: MetricScore?)

    class MetricScore(val raw: String?, val normalized: String?)
}
