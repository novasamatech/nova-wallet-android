package io.novafoundation.nova.feature_governance_impl.presentation.common.formatters

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.track.category.TrackCategorizer
import io.novafoundation.nova.feature_governance_impl.presentation.common.models.TrackModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class TrackFormatter(
    private val resourceManager: ResourceManager,
    private val trackCategorizer: TrackCategorizer,
) {

    fun formatTrack(trackId: TrackId, trackName: String, asset: Chain.Asset): TrackModel {
        return when (trackCategorizer.typeOf(trackName)) {
            TrackType.ROOT -> TrackModel(
                trackId = trackId,
                name = resourceManager.getString(R.string.referendum_track_root),
                icon = asset.iconUrl?.let { Icon.FromLink(it) } ?: Icon.FromDrawableRes(R.drawable.ic_block)
            )
            TrackType.WHITELISTED_CALLER -> TrackModel(
                trackId = trackId,
                name = resourceManager.getString(R.string.referendum_whitelisted_caller),
                icon = Icon.FromDrawableRes(R.drawable.ic_users)
            )
            TrackType.STAKING_ADMIN -> TrackModel(
                trackId = trackId,
                name = resourceManager.getString(R.string.referendum_staking_admin),
                icon = Icon.FromDrawableRes(R.drawable.ic_staking_filled)
            )
            TrackType.TREASURER -> TrackModel(
                trackId = trackId,
                name = resourceManager.getString(R.string.referendum_track_treasurer),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem)
            )
            TrackType.LEASE_ADMIN -> TrackModel(
                trackId = trackId,
                name = resourceManager.getString(R.string.referendum_track_lease_admin),
                icon = Icon.FromDrawableRes(R.drawable.ic_governance_check_to_slot)
            )
            TrackType.FELLOWSHIP_ADMIN -> TrackModel(
                trackId = trackId,
                name = resourceManager.getString(R.string.referendum_track_fellowship_admin),
                icon = Icon.FromDrawableRes(R.drawable.ic_users)
            )
            TrackType.GENERAL_ADMIN -> TrackModel(
                trackId = trackId,
                name = resourceManager.getString(R.string.referendum_track_general_admin),
                icon = Icon.FromDrawableRes(R.drawable.ic_governance_check_to_slot)
            )
            TrackType.AUCTION_ADMIN -> TrackModel(
                trackId = trackId,
                name = resourceManager.getString(R.string.referendum_track_auction_admin),
                icon = Icon.FromDrawableRes(R.drawable.ic_rocket)
            )
            TrackType.REFERENDUM_CANCELLER -> TrackModel(
                trackId = trackId,
                name = resourceManager.getString(R.string.referendum_track_referendum_canceller),
                icon = Icon.FromDrawableRes(R.drawable.ic_governance_check_to_slot)
            )
            TrackType.REFERENDUM_KILLER -> TrackModel(
                trackId = trackId,
                name = resourceManager.getString(R.string.referendum_track_referendum_killer),
                icon = Icon.FromDrawableRes(R.drawable.ic_governance_check_to_slot)
            )
            TrackType.SMALL_TIPPER -> TrackModel(
                trackId = trackId,
                name = resourceManager.getString(R.string.referendum_track_small_tipper),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem)
            )
            TrackType.BIG_TIPPER -> TrackModel(
                trackId = trackId,
                name = resourceManager.getString(R.string.referendum_track_big_tipper),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem)
            )
            TrackType.SMALL_SPEND -> TrackModel(
                trackId = trackId,
                name = resourceManager.getString(R.string.referendum_track_small_spender),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem)
            )
            TrackType.MEDIUM_SPEND -> TrackModel(
                trackId = trackId,
                name = resourceManager.getString(R.string.referendum_track_medium_spender),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem)
            )
            TrackType.BIG_SPEND -> TrackModel(
                trackId = trackId,
                name = resourceManager.getString(R.string.referendum_track_big_spender),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem)
            )
            TrackType.OTHER -> TrackModel(
                trackId = trackId,
                name = mapUnknownTrackNameToUi(trackName),
                icon = Icon.FromDrawableRes(R.drawable.ic_block)
            )
        }
    }

    private fun mapUnknownTrackNameToUi(name: String): String {
        return name.replace("_", " ").capitalize()
    }
}
