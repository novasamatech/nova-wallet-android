package io.novafoundation.nova.feature_push_notifications.data.presentation.governance

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.common.utils.updateValue
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksRequester
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.fromTrackIds
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.toTrackIds
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.ChainWithGovTracks
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.GovernancePushSettingsInteractor
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.adapter.PushGovernanceRVItem
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val MIN_TRACKS = 1

data class GovChainKey(val chainId: ChainId, val governance: Chain.Governance)

class PushGovernanceSettingsViewModel(
    private val router: PushNotificationsRouter,
    private val interactor: GovernancePushSettingsInteractor,
    private val pushGovernanceSettingsResponder: PushGovernanceSettingsResponder,
    private val chainRegistry: ChainRegistry,
    private val request: PushGovernanceSettingsRequester.Request,
    private val selectTracksRequester: SelectTracksRequester,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    private val chainsWithTracks = interactor.governanceChainsFlow()
        .shareInBackground()

    val _changedGovernanceSettingsList: MutableStateFlow<Map<GovChainKey, PushGovernanceModel>> = MutableStateFlow(emptyMap())

    val governanceSettingsList = combine(chainsWithTracks, _changedGovernanceSettingsList) { chainsWithTracksQuantity, changedSettings ->
        chainsWithTracksQuantity.map { chainAndTracks ->
            val pushSettingsModel = changedSettings[chainAndTracks.key()]
                ?: PushGovernanceModel.default(
                    chainAndTracks.chain,
                    chainAndTracks.govVersion,
                    chainAndTracks.tracks
                )

            PushGovernanceRVItem(
                pushSettingsModel,
                formatTracksText(pushSettingsModel.trackIds, chainAndTracks.tracks)
            )
        }
    }.withSafeLoading()

    init {
        launch {
            val chainsById = chainRegistry.chainsById()

            _changedGovernanceSettingsList.value = request.enabledGovernanceSettings
                .mapNotNull { chainIdToSettings ->
                    val chain = chainsById[chainIdToSettings.chainId] ?: return@mapNotNull null
                    mapCommunicatorModelToItem(chainIdToSettings, chain)
                }.associateBy { it.key() }
        }

        subscribeOnSelectTracks()
    }

    fun backClicked() {
        launch {
            val enabledGovernanceSettings = _changedGovernanceSettingsList.value
                .values
                .filter { it.isEnabled }
                .map { mapItemToCommunicatorModel(it) }

            val response = PushGovernanceSettingsResponder.Response(enabledGovernanceSettings)
            pushGovernanceSettingsResponder.respond(response)

            router.back()
        }
    }

    fun enableSwitcherClicked(item: PushGovernanceRVItem) {
        _changedGovernanceSettingsList.updateValue {
            it + item.model.copy(isEnabled = !item.isEnabled)
                .enableEverythingIfFeaturesDisabled()
                .withKey()
        }
    }

    fun newReferendaClicked(item: PushGovernanceRVItem) {
        _changedGovernanceSettingsList.updateValue {
            it + item.model.copy(isNewReferendaEnabled = !item.isNewReferendaEnabled)
                .disableCompletelyIfFeaturesDisabled()
                .withKey()
        }
    }

    fun referendaUpdatesClicked(item: PushGovernanceRVItem) {
        _changedGovernanceSettingsList.updateValue {
            it + item.model.copy(isReferendaUpdatesEnabled = !item.isReferendaUpdatesEnabled)
                .disableCompletelyIfFeaturesDisabled()
                .withKey()
        }
    }

    fun delegateVotesClicked(item: PushGovernanceRVItem) {
        _changedGovernanceSettingsList.updateValue {
            it + item.model.copy(isDelegationVotesEnabled = !item.isDelegationVotesEnabled)
                .disableCompletelyIfFeaturesDisabled()
                .withKey()
        }
    }

    fun tracksClicked(item: PushGovernanceRVItem) {
        launch {
            val selectedTracks = item.model.trackIds.mapToSet { it.value }
            selectTracksRequester.openRequest(SelectTracksRequester.Request(item.chainId, selectedTracks, MIN_TRACKS))
        }
    }

    fun clearClicked() {
        _changedGovernanceSettingsList.value = emptyMap()
    }

    private fun mapCommunicatorModelToItem(
        item: PushGovernanceSettingsPayload,
        chain: Chain
    ): PushGovernanceModel {
        val tracks = item.tracksIds.toTrackIds()
        return PushGovernanceModel(
            chainId = item.chainId,
            governance = item.governance,
            chainName = chain.name,
            chainIconUrl = chain.icon,
            isEnabled = true,
            isNewReferendaEnabled = item.newReferenda,
            isReferendaUpdatesEnabled = item.referendaUpdates,
            isDelegationVotesEnabled = item.delegateVotes,
            trackIds = tracks
        )
    }

    private fun mapItemToCommunicatorModel(item: PushGovernanceModel): PushGovernanceSettingsPayload {
        return PushGovernanceSettingsPayload(
            item.chainId,
            item.governance,
            item.isNewReferendaEnabled,
            item.isReferendaUpdatesEnabled,
            item.isDelegationVotesEnabled,
            item.trackIds.fromTrackIds()
        )
    }

    private fun PushGovernanceModel.disableCompletelyIfFeaturesDisabled(): PushGovernanceModel {
        if (!isNewReferendaEnabled && !isReferendaUpdatesEnabled && !isDelegationVotesEnabled) {
            return copy(isEnabled = false)
        }

        return this
    }

    private fun PushGovernanceModel.enableEverythingIfFeaturesDisabled(): PushGovernanceModel {
        if (!isNewReferendaEnabled && !isReferendaUpdatesEnabled && !isDelegationVotesEnabled) {
            return copy(isEnabled = true, isNewReferendaEnabled = true, isReferendaUpdatesEnabled = true, isDelegationVotesEnabled = true)
        }

        return this
    }

    private fun subscribeOnSelectTracks() {
        selectTracksRequester.responseFlow
            .onEach { response ->
                val governanceVersion = Chain.Governance.V2 // Since we can use track selecting only for OpenGov
                val chain = chainRegistry.getChain(response.chainId)
                val key = GovChainKey(response.chainId, governanceVersion)
                val selectedTracks = response.selectedTracks.toTrackIds()

                _changedGovernanceSettingsList.updateValue { governanceSettings ->
                    val model = governanceSettings[key]?.copy(trackIds = selectedTracks)
                        ?: PushGovernanceModel.default(
                            chain = chain,
                            governance = governanceVersion,
                            tracks = selectedTracks
                        )

                    governanceSettings.plus(key to model)
                }
            }
            .launchIn(this)
    }

    private fun PushGovernanceModel.key() = GovChainKey(chainId, governance)

    private fun PushGovernanceModel.withKey() = key() to this

    private fun ChainWithGovTracks.key() = GovChainKey(chain.id, govVersion)

    private fun formatTracksText(selectedTracks: Set<TrackId>, allTracks: Set<TrackId>): String {
        return if (selectedTracks.size == allTracks.size) {
            resourceManager.getString(R.string.common_all)
        } else {
            resourceManager.getString(R.string.selected_tracks_quantity, selectedTracks.size, allTracks.size)
        }
    }
}
