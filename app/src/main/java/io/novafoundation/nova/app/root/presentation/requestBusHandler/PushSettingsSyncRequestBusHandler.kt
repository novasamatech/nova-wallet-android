package io.novafoundation.nova.app.root.presentation.requestBusHandler

import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus.Event
import io.novafoundation.nova.feature_push_notifications.domain.interactor.PushNotificationsInteractor
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class PushSettingsSyncRequestBusHandler(
    private val scope: RootScope,
    private val metaAccountChangesEventBus: MetaAccountChangesEventBus,
    private val pushNotificationsInteractor: PushNotificationsInteractor
) : RequestBusHandler {

    override fun observe() {
        metaAccountChangesEventBus.observeEvent()
            .onEach { event ->
                when (event) {
                    is Event.AccountChanged -> pushNotificationsInteractor.onMetaAccountChanged(event.metaIds)
                    is Event.AccountRemoved -> pushNotificationsInteractor.onMetaAccountRemoved(event.metaId)
                    else -> {}
                }
            }.launchIn(scope)
    }
}
