package io.novafoundation.nova.feature_governance_impl.domain.track.category

import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackCategory
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.AUCTION_ADMIN
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.BIG_SPEND
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.BIG_TIPPER
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.FELLOWSHIP_ADMIN
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.GENERAL_ADMIN
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.LEASE_ADMIN
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.MEDIUM_SPEND
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.OTHER
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.REFERENDUM_CANCELLER
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.REFERENDUM_KILLER
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.ROOT
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.SMALL_SPEND
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.SMALL_TIPPER
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.STAKING_ADMIN
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.TREASURER
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.WHITELISTED_CALLER

interface TrackCategorizer {

    fun categoryOf(trackName: String): TrackCategory

    fun typeOf(trackName: String): TrackType
}

class RealTrackCategorizer : TrackCategorizer {

    override fun categoryOf(trackName: String): TrackCategory {
        return when (typeOf(trackName)) {
            FELLOWSHIP_ADMIN, WHITELISTED_CALLER -> TrackCategory.FELLOWSHIP

            TREASURER,
            SMALL_TIPPER, BIG_TIPPER,
            SMALL_SPEND, MEDIUM_SPEND, BIG_SPEND -> TrackCategory.TREASURY

            LEASE_ADMIN, GENERAL_ADMIN,
            REFERENDUM_KILLER, REFERENDUM_CANCELLER -> TrackCategory.GOVERNANCE

            else -> TrackCategory.OTHER
        }
    }

    override fun typeOf(trackName: String): TrackType {
        return when (trackName) {
            "root" -> ROOT
            "whitelisted_caller" -> WHITELISTED_CALLER
            "staking_admin" -> STAKING_ADMIN
            "treasurer" -> TREASURER
            "lease_admin" -> LEASE_ADMIN
            "fellowship_admin" -> FELLOWSHIP_ADMIN
            "general_admin" -> GENERAL_ADMIN
            "auction_admin" -> AUCTION_ADMIN
            "referendum_canceller" -> REFERENDUM_CANCELLER
            "referendum_killer" -> REFERENDUM_KILLER
            "small_tipper" -> SMALL_TIPPER
            "big_tipper" -> BIG_TIPPER
            "small_spender" -> SMALL_SPEND
            "medium_spender" -> MEDIUM_SPEND
            "big_spender" -> BIG_SPEND
            else -> OTHER
        }
    }
}
