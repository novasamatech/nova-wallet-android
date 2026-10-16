package io.novafoundation.nova.feature_staking_impl.data.bittensor

import java.math.BigInteger

class BittensorValidatorsRemote(val items: List<Item>?) {
    class Item(val netuid: Int?, val hotkey: String?, val identitySourceName: String?, val stakeSourceName: String?)
}

class BittensorRecommendationsRemote(val meta: Meta?, val classes: Classes? = null) {
    class Meta(val clientGates: Gates?, val components: Components?, val generation: Generation? = null)
    class Generation(val sourceBlockNumber: BigInteger?)
    class Classes(val stable: List<Item>?, val balanced: List<Item>?, val higherUpside: List<Item>?)
    class Item(val netuid: Int?, val hotkey: String?)
    class Components(val recommendations: Component?)
    class Component(val availability: String?, val freshness: String?)
    class Gates(val maxTake: String?, val requirePermit: Boolean?, val requireActiveWithinCutoff: Boolean?)
}
