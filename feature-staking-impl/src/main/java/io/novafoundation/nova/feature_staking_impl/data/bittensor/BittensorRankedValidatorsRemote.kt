package io.novafoundation.nova.feature_staking_impl.data.bittensor

class BittensorRankedValidatorsRemote(
    val meta: BittensorRecommendationsRemote.Meta?,
    val items: List<Item>?
) {
    class Item(
        val netuid: Int?,
        val hotkey: String?,
        val validatorName: String?,
        val eligible: Boolean?,
        val recommendationClass: String?,
        val score: String?
    )
}
