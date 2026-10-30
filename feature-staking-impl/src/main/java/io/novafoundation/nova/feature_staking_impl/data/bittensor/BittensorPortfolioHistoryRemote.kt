package io.novafoundation.nova.feature_staking_impl.data.bittensor

class BittensorPortfolioHistoryRemote(
    val period: String?,
    val window: Window?,
    val points: List<Point>?,
    val historyScope: String?,
    val meta: Meta?
) {
    class Window(val start: String?, val end: String?)

    class Point(val timestamp: String?, val reportedValueTao: String?, val reportedValueUsd: String?, val completed: Boolean?)

    class Meta(val completeness: String?, val components: Components?)

    class Components(val portfolioHistory: Component?)

    class Component(
        val asOf: String?,
        val freshness: String?,
        val availability: String?,
        val valueQuality: String?,
        val sourceClass: String?
    )
}
