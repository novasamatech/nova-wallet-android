package io.novafoundation.nova.feature_push_notifications.data.presentation.settings

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.resources.mapBooleanToState
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.utils.updateValue
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettings
import io.novafoundation.nova.feature_push_notifications.data.data.settings.isAnyGovEnabled
import io.novafoundation.nova.feature_push_notifications.data.data.settings.isNotEmpty
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.WelcomePushNotificationsInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class PushSettingsViewModel(
    private val router: PushNotificationsRouter,
    private val pushNotificationsInteractor: PushNotificationsInteractor,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    private val _switchingInProgress = MutableStateFlow(false)

    val pushEnabledState = MutableStateFlow(pushNotificationsInteractor.isPushNotificationsEnabled())
    private val pushSettingsState = MutableStateFlow<PushSettings?>(null)

    val pushWalletsQuantity = pushSettingsState
        .mapNotNull { it?.wallets?.size?.format() }
        .distinctUntilChanged()

    val pushAnnouncements = pushSettingsState
        .mapNotNull { it?.announcementsEnabled }
        .distinctUntilChanged()

    val pushSentTokens = pushSettingsState
        .mapNotNull { it?.sentTokensEnabled }
        .distinctUntilChanged()

    val pushReceivedTokens = pushSettingsState
        .mapNotNull { it?.receivedTokensEnabled }
        .distinctUntilChanged()

    val pushGovernanceState = pushSettingsState
        .mapNotNull { it }
        .map { resourceManager.mapBooleanToState(it.isAnyGovEnabled()) }
        .distinctUntilChanged()

    val pushStakingRewardsState = pushSettingsState
        .mapNotNull { it }
        .map { resourceManager.mapBooleanToState(it.stakingReward.isNotEmpty()) }
        .distinctUntilChanged()

    init {
        launch {
            pushSettingsState.value = pushNotificationsInteractor.getPushSettings()
        }
    }

    fun backClicked() {
        router.back()
    }

    fun saveClicked() {
        launch {
            val pushSettings = pushSettingsState.value ?: return@launch
            pushNotificationsInteractor.updatePushSettings(pushEnabledState.value, pushSettings)

            router.back()
        }
    }

    fun enableSwitcherClicked() {
        pushEnabledState.toggle()
    }

    fun walletsClicked() {
        TODO()
    }

    fun announementsClicked() {
        pushSettingsState.updateValue { it?.copy(announcementsEnabled = !it.announcementsEnabled) }
    }

    fun sentTokensClicked() {
        pushSettingsState.updateValue { it?.copy(sentTokensEnabled = !it.sentTokensEnabled) }
    }

    fun receivedTokensClicked() {
        pushSettingsState.updateValue { it?.copy(receivedTokensEnabled = !it.receivedTokensEnabled) }
    }

    fun governanceClicked() {
        TODO()
    }

    fun stakingRewardsClicked() {
        TODO()
    }
}
