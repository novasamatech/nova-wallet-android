package io.novafoundation.nova.feature_push_notifications.presentation.multisigsWarning

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.domain.interactor.MultisigPushAlertInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MultisigPushNotificationsAlertMixinFactory(
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val interactor: MultisigPushAlertInteractor,
    private val metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry,
    private val router: PushNotificationsRouter
) {
    fun create(coroutineScope: CoroutineScope): MultisigPushNotificationsAlertMixin {
        return RealMultisigPushNotificationsAlertMixin(
            automaticInteractionGate,
            interactor,
            metaAccountsUpdatesRegistry,
            router,
            coroutineScope
        )
    }
}

interface MultisigPushNotificationsAlertMixin {

    val showAlertEvent: LiveData<Event<Unit>>

    fun subscribeToShowAlert()

    fun showPushSettings()
}

class RealMultisigPushNotificationsAlertMixin(
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val interactor: MultisigPushAlertInteractor,
    private val metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry,
    private val router: PushNotificationsRouter,
    private val coroutineScope: CoroutineScope
) : MultisigPushNotificationsAlertMixin {

    override val showAlertEvent = MutableLiveData<Event<Unit>>()

    override fun subscribeToShowAlert() = coroutineScope.launchUnit {
        if (interactor.isAlertAlreadyShown()) return@launchUnit

        // We should get this state before multisigs will be discovered so we call this method before interaction gate
        val allowedToShowAlertAtStart = interactor.allowedToShowAlertAtStart()

        automaticInteractionGate.awaitInteractionAllowed()

        if (allowedToShowAlertAtStart) {
            showAlert()
            return@launchUnit
        }

        // We have to show alert after user saw new multisigs in account list so we subscribed to its update states
        // And show alert when at least one multisig update was consumed
        metaAccountsUpdatesRegistry.observeLastConsumedUpdatesMetaIds()
            .onEach { consumedMetaIdsUpdates ->
                if (interactor.isAlertAlreadyShown()) return@onEach

                if (interactor.hasMultisigWallets(consumedMetaIdsUpdates.toList())) {
                    // We need to check interaction again since app may went to background before consuming updates
                    automaticInteractionGate.awaitInteractionAllowed()
                    showAlert()
                }
            }
            .launchIn(coroutineScope)
    }

    private fun showAlert() {
        interactor.setAlertWasAlreadyShown()
        showAlertEvent.sendEvent()
    }

    override fun showPushSettings() {
        router.openPushSettingsWithAccounts()
    }
}
