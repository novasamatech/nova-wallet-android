package io.novafoundation.nova.feature_push_notifications.presentation.multisigs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.domain.model.disableIfAllTypesDisabled
import io.novafoundation.nova.feature_push_notifications.domain.model.isAllTypesDisabled
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class PushMultisigSettingsViewModel(
    private val router: PushNotificationsRouter,
    private val pushMultisigSettingsResponder: PushMultisigSettingsResponder,
    private val request: PushMultisigSettingsRequester.Request,
    private val appLinksProvider: AppLinksProvider
) : BaseViewModel(), Browserable {

    private val settingsState = MutableStateFlow(request.settings.toDomain())

    val isMultisigNotificationsEnabled = settingsState.map { it.isEnabled }
        .distinctUntilChanged()

    val isInitiationEnabled = settingsState.map { it.isInitiatingEnabled }
        .distinctUntilChanged()

    val isApprovingEnabled = settingsState.map { it.isApprovingEnabled }
        .distinctUntilChanged()

    val isExecutionEnabled = settingsState.map { it.isExecutionEnabled }
        .distinctUntilChanged()

    val isRejectionEnabled = settingsState.map { it.isRejectionEnabled }
        .distinctUntilChanged()

    private val _noOneMultisigWalletSelectedEvent = MutableLiveData<Event<Unit>>()
    val noOneMultisigWalletSelectedEvent: LiveData<Event<Unit>> = _noOneMultisigWalletSelectedEvent

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    fun backClicked() {
        pushMultisigSettingsResponder.respond(PushMultisigSettingsResponder.Response(settingsState.value.toModel()))
        router.back()
    }

    fun switchMultisigNotificationsState() {
        val noMultisigWalletSelected = !request.isAtLeastOneMultisigWalletSelected
        if (noMultisigWalletSelected) {
            _noOneMultisigWalletSelectedEvent.sendEvent()
            return
        }

        toggleMultisigEnablingState()
    }

    private fun toggleMultisigEnablingState() {
        settingsState.update {
            if (!it.isEnabled && it.isAllTypesDisabled()) {
                it.copy(isEnabled = true, isInitiatingEnabled = true, isApprovingEnabled = true, isExecutionEnabled = true, isRejectionEnabled = true)
            } else {
                it.copy(isEnabled = !it.isEnabled)
            }
        }
    }

    fun switchInitialNotificationsState() {
        settingsState.update {
            it.copy(isInitiatingEnabled = !it.isInitiatingEnabled)
                .disableIfAllTypesDisabled()
        }
    }

    fun switchApprovingNotificationsState() {
        settingsState.update {
            it.copy(isApprovingEnabled = !it.isApprovingEnabled)
                .disableIfAllTypesDisabled()
        }
    }

    fun switchExecutionNotificationsState() {
        settingsState.update {
            it.copy(isExecutionEnabled = !it.isExecutionEnabled)
                .disableIfAllTypesDisabled()
        }
    }

    fun switchRejectionNotificationsState() {
        settingsState.update {
            it.copy(isRejectionEnabled = !it.isRejectionEnabled)
                .disableIfAllTypesDisabled()
        }
    }

    fun learnMoreClicked() {
        openBrowserEvent.value = Event(appLinksProvider.multisigsWikiUrl)
    }
}
