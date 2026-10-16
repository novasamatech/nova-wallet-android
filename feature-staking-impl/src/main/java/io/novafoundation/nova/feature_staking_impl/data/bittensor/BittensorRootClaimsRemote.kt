package io.novafoundation.nova.feature_staking_impl.data.bittensor

class BittensorRootClaimsRemote(val items: List<Item>?, val historyScope: String?, val meta: Meta?) {
    class Item(val block: Long?, val extrinsicIndex: Int?, val eventIndex: Int?, val hotkey: String?, val payoutTao: String?)
    class Meta(val components: Components?)
    class Components(val rootClaims: BittensorRecommendationsRemote.Component?)
}
