package io.novafoundation.nova.feature_staking_impl.data.bittensor

class BittensorAlphaYieldsRemote(
    val items: List<Item>?,
    val meta: Meta?,
    val pageInfo: BittensorHistoryPageInfo?
) {
    class Item(
        val netuid: Int?,
        val hotkey: String?,
        val reportedRate: String?,
        val metricKind: String?,
        /** The server's verdict on [reportedRate] (ADR-0045): `RELIABLE` or `UNRELIABLE`; absent on an older backend. */
        val rateReliability: String? = null,
        /** Why the rate is not reliable; an open lower-snake-case vocabulary. */
        val rateFlags: List<String>? = null
    )
    class Meta(val components: Components?)
    class Components(val alphaYield: BittensorRecommendationsRemote.Component?)

    companion object {
        const val RELIABLE_RATE = "RELIABLE"
    }
}

class BittensorHistoryPageInfo(val page: Int?, val nextPage: Int?)

class BittensorOperationsRemote(
    val items: List<Item>?,
    val historyScope: String?,
    val meta: Meta?,
    val pageInfo: BittensorHistoryPageInfo?
) {
    class Item(
        val netuid: Int?,
        val hotkey: String?,
        val reportedAmountIn: String?,
        val reportedAmountOut: String?,
        val reportedPrice: String?,
        val sourceEventId: String?,
        val sourceTimestamp: String?,
        val sourceExtrinsicReference: String?,
        val sourceOperationType: String?
    )
    class Meta(val components: Components?)
    class Components(val operationHistory: BittensorRecommendationsRemote.Component?)
}
