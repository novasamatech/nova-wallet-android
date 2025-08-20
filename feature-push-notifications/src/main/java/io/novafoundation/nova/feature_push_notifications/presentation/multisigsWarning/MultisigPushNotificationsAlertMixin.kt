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

interface MultisigPushNotificationsAlertMixin {

    val showWarningEvent: LiveData<Event<Unit>>

    fun subscribeToShowAlert(coroutineScope: CoroutineScope)

    fun showPushSettings()
}

class RealMultisigPushNotificationsAlertMixin(
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val interactor: MultisigPushAlertInteractor,
    private val metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry,
    private val router: PushNotificationsRouter
) : MultisigPushNotificationsAlertMixin {

    override val showWarningEvent = MutableLiveData<Event<Unit>>()

    override fun subscribeToShowAlert(coroutineScope: CoroutineScope) = coroutineScope.launchUnit {
        if (interactor.isAlertWasAlreadyShown()) return@launchUnit

        //We should get this state before multisigs will be discovered so we call this method before interaction gate
        val allowedToShowAlertAtStart = interactor.allowedToShowAlertAtStart()

        automaticInteractionGate.awaitInteractionAllowed()

        if (allowedToShowAlertAtStart) {
            interactor.setAlertWasAlreadyShown()
            showWarningEvent.sendEvent()
            return@launchUnit
        }

        // We have to show alert after user saw new multisigs in account list so we subscribed to its update states
        // And show alert when at least one multisig update was consumed
        metaAccountsUpdatesRegistry.observeConsumedUpdatesMetaIds()
            .onEach { consumedMetaIdsUpdates ->
                if (interactor.isAlertWasAlreadyShown()) return@onEach

                if (interactor.hasMultisigWallets(consumedMetaIdsUpdates.toList())) {
                    interactor.setAlertWasAlreadyShown()
                    showWarningEvent.sendEvent()
                }
            }
            .launchIn(coroutineScope)
    }

    override fun showPushSettings() {
        if (automaticInteractionGate.isInteractionAllowed()) {
            router.openPushSettingsWithAccounts()
        }
    }
}
