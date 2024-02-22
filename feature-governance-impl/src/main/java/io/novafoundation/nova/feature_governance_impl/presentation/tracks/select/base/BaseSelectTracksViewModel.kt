@file:Suppress("LeakingThis")

package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model.ChooseTrackData
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model.TrackPreset
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.model.DelegationTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.model.DelegationTracksPresetModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

abstract class BaseSelectTracksViewModel(
    private val trackFormatter: TrackFormatter,
    private val resourceManager: ResourceManager,
    private val router: GovernanceRouter,
    chooseTrackDataFlow: Flow<ChooseTrackData>
) : BaseViewModel() {

    protected val chooseTrackDataFlowShared = chooseTrackDataFlow
        .inBackground()
        .shareWhileSubscribed()

    protected val selectedTracksFlow = MutableStateFlow(setOf<TrackId>())

    private val trackPresetsFlow = chooseTrackDataFlowShared.map { it.presets }
        .shareWhileSubscribed()

    private val availableTrackFlow = chooseTrackDataFlowShared.map { it.trackPartition.available }
        .shareWhileSubscribed()

    val trackPresetsModels = trackPresetsFlow
        .map(::mapTrackPresets)
        .shareWhileSubscribed()

    val availableTrackModels = combine(availableTrackFlow, selectedTracksFlow, ::mapTracksToModel)
        .withSafeLoading()
        .shareWhileSubscribed()

    abstract suspend fun getChainAsset(): Chain.Asset

    open fun backClicked() {
        router.back()
    }

    fun trackClicked(position: Int) {
        launch {
            val track = availableTrackFlow.first()[position]
            val selectedTracks = selectedTracksFlow.value
            selectedTracksFlow.value = selectedTracks.toggle(track.id)
        }
    }

    fun presetClicked(position: Int) {
        launch {
            val selectedPreset = trackPresetsFlow.first()[position]
            selectedTracksFlow.value = selectedPreset.trackIds.toHashSet()
        }
    }

    private fun mapTrackPresets(trackPresets: List<TrackPreset>): List<DelegationTracksPresetModel> {
        return trackPresets.map {
            DelegationTracksPresetModel(
                label = mapPresetTypeToButtonName(it.type),
                trackPresetModels = it.type
            )
        }
    }

    private fun mapPresetTypeToButtonName(trackPresetType: TrackPreset.Type): String {
        return when (trackPresetType) {
            TrackPreset.Type.ALL -> resourceManager.getString(R.string.delegation_tracks_all_preset)
            TrackPreset.Type.FELLOWSHIP -> resourceManager.getString(R.string.delegation_tracks_fellowship_preset)
            TrackPreset.Type.TREASURY -> resourceManager.getString(R.string.delegation_tracks_treasury_preset)
            TrackPreset.Type.GOVERNANCE -> resourceManager.getString(R.string.delegation_tracks_governance_preset)
        }
    }

    private suspend fun mapTracksToModel(tracks: List<Track>, selectedTracks: Set<TrackId>): List<DelegationTrackModel> {
        return tracks.map {
            val trackModel = trackFormatter.formatTrack(it, getChainAsset())
            DelegationTrackModel(
                trackModel,
                isSelected = it.id in selectedTracks
            )
        }
    }
}
