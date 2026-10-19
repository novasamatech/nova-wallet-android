package io.novafoundation.nova.feature_staking_impl.data.bittensor

internal fun portfolioHistoryResponse(
    period: String = "THIRTY_DAYS",
    points: List<BittensorPortfolioHistoryRemote.Point> = emptyList(),
    freshness: String = "FRESH",
    start: String = "2026-09-01T00:00:00Z",
    end: String = "2026-10-01T00:00:00Z",
    scope: String = "TAO_APP_PARTIAL"
): BittensorPortfolioHistoryRemote {
    val component = BittensorPortfolioHistoryRemote.Component(end, freshness, "AVAILABLE", "DERIVED", "PRIMARY")
    val components = BittensorPortfolioHistoryRemote.Components(component)
    val meta = BittensorPortfolioHistoryRemote.Meta("COMPLETE", components)
    val window = BittensorPortfolioHistoryRemote.Window(start, end)
    return BittensorPortfolioHistoryRemote(period, window, points, scope, meta)
}
