package io.novafoundation.nova.feature_push_notifications.data.presentation.governance

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.updateValue
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksRequester
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.ChainWithGovTracks
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.GovernancePushSettingsInteractor
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.adapter.PushGovernanceRVItem
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.adapter.default
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class GovChainKey(val chainId: ChainId, val governance: Chain.Governance)

class PushGovernanceSettingsViewModel(
    private val router: PushNotificationsRouter,
    private val interactor: GovernancePushSettingsInteractor,
    private val pushGovernanceSettingsResponder: PushGovernanceSettingsResponder,
    private val chainRegistry: ChainRegistry,
    private val request: PushGovernanceSettingsRequester.Request,
    private val selectTracksRequester: SelectTracksRequester
) : BaseViewModel() {

    private val chainsWithTracksQuantity = interactor.governanceChainsFlow()
        .shareInBackground()

    val _changedGovernanceSettingsLsit: MutableStateFlow<Map<GovChainKey, PushGovernanceRVItem>> = MutableStateFlow(emptyMap())

    val governanceSettingsList = combine(chainsWithTracksQuantity, _changedGovernanceSettingsLsit) { chainsWithTracksQuantity, changedSettings ->
        chainsWithTracksQuantity.map { chainWithTracksQuantity ->
            val changedItem = changedSettings[chainWithTracksQuantity.key()]
            changedItem ?: PushGovernanceRVItem.default(chainWithTracksQuantity.chain, chainWithTracksQuantity.govVersion)
        }
    }.withSafeLoading()

    init {
        launch {
            val chainsById = chainRegistry.chainsById()

            _changedGovernanceSettingsLsit.value = request.enabledGovernanceSettings
                .mapNotNull { chainIdToSettings ->
                    val chain = chainsById[chainIdToSettings.chainId] ?: return@mapNotNull null
                    mapCommunicatorModelToItem(chainIdToSettings, chain)
                }.associateBy { it.key() }
        }

        subscribeOnSelectTracks()
    }

    private fun subscribeOnSelectTracks() {
        selectTracksRequester.responseFlow
            .onEach {
                // set tracks to item
            }
            .launchIn(this)
    }

    fun backClicked() {
        launch {
            val enabledGovernanceSettings = _changedGovernanceSettingsLsit.value
                .values
                .filter { it.isEnabled }
                .map { mapItemToCommunicatorModel(it) }

            val response = PushGovernanceSettingsResponder.Response(enabledGovernanceSettings)
            pushGovernanceSettingsResponder.respond(response)
        }
        router.back()
    }

    fun enableSwitcherClicked(item: PushGovernanceRVItem) {
        _changedGovernanceSettingsLsit.updateValue {
            it + item.copy(isEnabled = !item.isEnabled)
                .enableEverythingIfFeaturesDisabled()
                .withKey()
        }
    }

    fun newReferendaClicked(item: PushGovernanceRVItem) {
        _changedGovernanceSettingsLsit.updateValue {
            it + item.copy(isNewReferendaEnabled = !item.isNewReferendaEnabled)
                .disableCompletelyIfFeaturesDisabled()
                .withKey()
        }
    }

    fun referendaUpdatesClicked(item: PushGovernanceRVItem) {
        _changedGovernanceSettingsLsit.updateValue {
            it + item.copy(isReferendaUpdatesEnabled = !item.isReferendaUpdatesEnabled)
                .disableCompletelyIfFeaturesDisabled()
                .withKey()
        }
    }

    fun delegateVotesClicked(item: PushGovernanceRVItem) {
        _changedGovernanceSettingsLsit.updateValue {
            it + item.copy(isDelegationVotesEnabled = !item.isDelegationVotesEnabled)
                .disableCompletelyIfFeaturesDisabled()
                .withKey()
        }
    }

    fun tracksClicked(item: PushGovernanceRVItem) {
        selectTracksRequester.openRequest(SelectTracksRequester.Request(item.chainId, emptySet()))
    }

    private fun mapCommunicatorModelToItem(item: PushGovernanceSettings, chain: Chain): PushGovernanceRVItem {
        return PushGovernanceRVItem(
            chainId = item.chainId,
            governance = item.governance,
            chainName = chain.name,
            chainIconUrl = chain.icon,
            isEnabled = true,
            isNewReferendaEnabled = item.newReferenda,
            isReferendaUpdatesEnabled = item.referendaUpdates,
            isDelegationVotesEnabled = item.delegateVotes,
            tracks = when (item.tracks) {
                is PushGovernanceSettings.Tracks.All -> PushGovernanceRVItem.Tracks.All
                is PushGovernanceSettings.Tracks.Specified -> PushGovernanceRVItem.Tracks.Specified(item.tracks.items, 0)
            }
        )
    }

    private fun mapItemToCommunicatorModel(item: PushGovernanceRVItem): PushGovernanceSettings {
        return PushGovernanceSettings(
            item.chainId,
            item.governance,
            item.isNewReferendaEnabled,
            item.isReferendaUpdatesEnabled,
            item.isDelegationVotesEnabled,
            tracks = when (item.tracks) {
                is PushGovernanceRVItem.Tracks.All -> PushGovernanceSettings.Tracks.All
                is PushGovernanceRVItem.Tracks.Specified -> PushGovernanceSettings.Tracks.Specified(item.tracks.items)
            }
        )
    }

    private fun PushGovernanceRVItem.disableCompletelyIfFeaturesDisabled(): PushGovernanceRVItem {
        if (!isNewReferendaEnabled && !isReferendaUpdatesEnabled && !isDelegationVotesEnabled)
            return copy(isEnabled = false)

        return this
    }

    private fun PushGovernanceRVItem.enableEverythingIfFeaturesDisabled(): PushGovernanceRVItem {
        if (!isNewReferendaEnabled && !isReferendaUpdatesEnabled && !isDelegationVotesEnabled)
            return copy(isEnabled = true, isNewReferendaEnabled = true, isReferendaUpdatesEnabled = true, isDelegationVotesEnabled = true)

        return this
    }

    private fun PushGovernanceRVItem.key() = GovChainKey(chainId, governance)

    private fun PushGovernanceRVItem.withKey() = key() to this

    private fun ChainWithGovTracks.key() = GovChainKey(chain.id, govVersion)
}
