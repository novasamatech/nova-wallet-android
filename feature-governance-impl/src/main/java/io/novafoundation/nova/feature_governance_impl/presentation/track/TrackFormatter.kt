package io.novafoundation.nova.feature_governance_impl.presentation.track

import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.resources.formatListPreview
import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.track.category.TrackCategorizer
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface TrackFormatter {

    fun formatTrack(track: Track, asset: Chain.Asset): TrackModel

    fun formatTracksSummary(tracks: Collection<Track>, asset: Chain.Asset): String
}

fun TrackFormatter.formatTracks(tracks: List<Track>, asset: Chain.Asset): TracksModel {
    val trackModels = tracks.map { formatTrack(it, asset) }
    val overview = formatTracksSummary(tracks, asset)

    return TracksModel(trackModels, overview)
}

class RealTrackFormatter(
    private val trackCategorizer: TrackCategorizer,
    private val resourceManager: ResourceManager,
    private val assetIconProvider: AssetIconProvider
) : TrackFormatter {

    override fun formatTrack(track: Track, asset: Chain.Asset): TrackModel {
        return when (trackCategorizer.typeOf(track.name)) {
            TrackType.ROOT -> TrackModel(
                name = resourceManager.getString(R.string.referendum_track_root),
                icon = assetIconProvider.getAssetIconOrFallback(asset, fallbackIcon = R.drawable.ic_block.asIcon()),
            )

            TrackType.WHITELISTED_CALLER -> TrackModel(
                name = resourceManager.getString(R.string.referendum_whitelisted_caller),
                icon = Icon.FromDrawableRes(R.drawable.ic_users),
            )

            TrackType.STAKING_ADMIN -> TrackModel(
                name = resourceManager.getString(R.string.referendum_staking_admin),
                icon = Icon.FromDrawableRes(R.drawable.ic_staking_filled),
            )

            TrackType.TREASURER -> TrackModel(
                name = resourceManager.getString(R.string.referendum_track_treasurer),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem),
            )

            TrackType.LEASE_ADMIN -> TrackModel(
                name = resourceManager.getString(R.string.referendum_track_lease_admin),
                icon = Icon.FromDrawableRes(R.drawable.ic_governance_check_to_slot),
            )

            TrackType.FELLOWSHIP_ADMIN -> TrackModel(
                name = resourceManager.getString(R.string.referendum_track_fellowship_admin),
                icon = Icon.FromDrawableRes(R.drawable.ic_users),
            )

            TrackType.GENERAL_ADMIN -> TrackModel(
                name = resourceManager.getString(R.string.referendum_track_general_admin),
                icon = Icon.FromDrawableRes(R.drawable.ic_governance_check_to_slot),
            )

            TrackType.AUCTION_ADMIN -> TrackModel(
                name = resourceManager.getString(R.string.referendum_track_auction_admin),
                icon = Icon.FromDrawableRes(R.drawable.ic_rocket),
            )

            TrackType.REFERENDUM_CANCELLER -> TrackModel(
                name = resourceManager.getString(R.string.referendum_track_referendum_canceller),
                icon = Icon.FromDrawableRes(R.drawable.ic_governance_check_to_slot),
            )

            TrackType.REFERENDUM_KILLER -> TrackModel(
                name = resourceManager.getString(R.string.referendum_track_referendum_killer),
                icon = Icon.FromDrawableRes(R.drawable.ic_governance_check_to_slot),
            )

            TrackType.SMALL_TIPPER -> TrackModel(
                name = resourceManager.getString(R.string.referendum_track_small_tipper),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem),
            )

            TrackType.BIG_TIPPER -> TrackModel(
                name = resourceManager.getString(R.string.referendum_track_big_tipper),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem),
            )

            TrackType.SMALL_SPEND -> TrackModel(
                name = resourceManager.getString(R.string.referendum_track_small_spender),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem),
            )

            TrackType.MEDIUM_SPEND -> TrackModel(
                name = resourceManager.getString(R.string.referendum_track_medium_spender),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem),
            )

            TrackType.BIG_SPEND -> TrackModel(
                name = resourceManager.getString(R.string.referendum_track_big_spender),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem),
            )

            TrackType.OTHER -> TrackModel(
                name = mapUnknownTrackNameToUi(track.name),
                icon = Icon.FromDrawableRes(R.drawable.ic_block),
            )
        }
    }

    override fun formatTracksSummary(tracks: Collection<Track>, asset: Chain.Asset): String {
        val trackLabels = tracks.map { formatTrack(it, asset).name }

        return resourceManager.formatListPreview(trackLabels)
    }

    private fun mapUnknownTrackNameToUi(name: String): String {
        return name.replace("_", " ").capitalize()
    }
}
