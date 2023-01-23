package io.novafoundation.nova.feature_governance_api.domain.referendum.track.category

enum class TrackCategory {

    TREASURY, GOVERNANCE, FELLOWSHIP, OTHER
}

enum class TrackType {
    ROOT,
    WHITELISTED_CALLER, FELLOWSHIP_ADMIN,
    STAKING_ADMIN, LEASE_ADMIN, AUCTION_ADMIN,
    GENERAL_ADMIN, REFERENDUM_CANCELLER, REFERENDUM_KILLER,
    TREASURER, SMALL_TIPPER, BIG_TIPPER, SMALL_SPEND, MEDIUM_SPEND, BIG_SPEND,
    OTHER
}
