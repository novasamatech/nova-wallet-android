package io.novafoundation.nova.feature_staking_impl.data.bittensor

import java.math.BigInteger

class BittensorValidatorsRemote(val items: List<Item>?, val meta: Meta? = null) {
    class Meta(val completeness: String?, val components: Components?)

    class Components(
        val validatorStakes: BittensorRecommendationsRemote.Component?,
        val validatorMetagraph: BittensorRecommendationsRemote.Component?,
        val validatorIdentities: BittensorRecommendationsRemote.Component?
    )

    class Item(
        val netuid: Int?,
        val hotkey: String?,
        val identitySourceName: String?,
        val stakeSourceName: String?,
        val reportedMeasurements: ReportedMeasurements? = null
    )

    class ReportedMeasurements(val validatorStake: String?)
}

class BittensorRecommendationsRemote(
    val meta: Meta?,
    val classes: Classes? = null,
    val items: List<BittensorSubnetRankingRemote>? = null
) {
    class Meta(val clientGates: Gates?, val components: Components?, val generation: Generation? = null)
    class Generation(val sourceBlockNumber: BigInteger?, val carriedOverNetuids: List<Int>? = null)
    class Classes(val stable: List<Item>?, val balanced: List<Item>?, val higherUpside: List<Item>?)
    class Item(val netuid: Int?, val hotkey: String?)
    class Components(
        val recommendations: Component?,
        val rootValidator: Component? = null,
        val rootClaims: Component? = null,
        val alphaYield: Component? = null,
        val operationHistory: Component? = null
    )
    class Component(
        val availability: String?,
        val freshness: String?,
        val asOf: String? = null,
        val availabilityReason: String? = null
    )
    class Gates(val maxTake: String?, val requirePermit: Boolean?, val requireActiveWithinCutoff: Boolean?)
}
