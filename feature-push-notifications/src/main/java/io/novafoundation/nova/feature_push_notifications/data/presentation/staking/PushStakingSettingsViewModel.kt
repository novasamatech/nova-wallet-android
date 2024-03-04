package io.novafoundation.nova.feature_push_notifications.data.presentation.staking

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.utils.updateValue
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.StakingPushSettingsInteractor
import io.novafoundation.nova.feature_push_notifications.data.presentation.staking.adapter.PushStakingRVItem
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PushStakingSettingsViewModel(
    private val router: PushNotificationsRouter,
    private val pushStakingSettingsResponder: PushStakingSettingsCommunicator,
    private val chainRegistry: ChainRegistry,
    private val request: PushStakingSettingsRequester.Request,
    private val resourceManager: ResourceManager,
    private val stakingPushSettingsInteractor: StakingPushSettingsInteractor
) : BaseViewModel() {

    private val chainsFlow = stakingPushSettingsInteractor.stakingChainsFlow()

    val _enabledStakingSettingsList: MutableStateFlow<Set<ChainId>> = MutableStateFlow(emptySet())

    val stakingSettingsList = combine(chainsFlow, _enabledStakingSettingsList) { chains, enabledChains ->
        chains.map { chain ->
            PushStakingRVItem(
                chain.id,
                chain.name,
                chain.icon,
                enabledChains.contains(chain.id)
            )
        }
    }.withSafeLoading()
        .shareInBackground()

    val clearButtonEnabledFlow = _enabledStakingSettingsList.map {
        it.isNotEmpty()
    }.shareInBackground()

    init {
        launch {
            _enabledStakingSettingsList.value = when (request.settings) {
                PushStakingSettingsPayload.AllChains -> chainsFlow.first().mapToSet { it.id }

                is PushStakingSettingsPayload.SpecifiedChains -> request.settings.enabledChainIds
            }
        }
    }

    fun backClicked() {
        launch {
            val allChainsIds = chainsFlow.first().mapToSet { it.id }
            val enabledChains = _enabledStakingSettingsList.value

            val settings = if (enabledChains == allChainsIds) {
                PushStakingSettingsPayload.AllChains
            } else {
                PushStakingSettingsPayload.SpecifiedChains(enabledChains)
            }

            val response = PushStakingSettingsResponder.Response(settings)
            pushStakingSettingsResponder.respond(response)

            router.back()
        }
    }

    fun clearClicked() {
        _enabledStakingSettingsList.value = emptySet()
    }

    fun itemClicked(item: PushStakingRVItem) {
        _enabledStakingSettingsList.updateValue {
            it.toggle(item.chainId)
        }
    }
}
