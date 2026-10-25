package io.novafoundation.nova.feature_staking_impl.data.bittensor

/**
 * Backend components are usable while AVAILABLE, whether FRESH or STALE (iOS renders stale data and marks its age).
 * Only an UNAVAILABLE or malformed component is rejected. Returns whether the component is FRESH.
 */
internal fun requireAvailableBittensorComponent(component: BittensorRecommendationsRemote.Component?, name: String): Boolean {
    val metadata = requireNotNull(component) { "Missing $name metadata" }
    require(metadata.availability == "AVAILABLE") { "$name is unavailable" }
    return when (metadata.freshness) {
        "FRESH" -> true
        "STALE" -> false
        else -> throw IllegalArgumentException("Unknown $name freshness")
    }
}

/** Whether the recommendation generation is FRESH. A STALE generation still recommends; the validator list shows it as outdated. */
internal fun hasFreshBittensorRecommendations(meta: BittensorRecommendationsRemote.Meta?): Boolean =
    meta?.components?.recommendations?.let { it.availability == "AVAILABLE" && it.freshness == "FRESH" } == true
