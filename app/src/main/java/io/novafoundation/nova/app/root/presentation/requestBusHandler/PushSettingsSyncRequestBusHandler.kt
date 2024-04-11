package io.novafoundation.nova.app.root.presentation.requestBusHandler

import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.events.collect
import io.novafoundation.nova.feature_account_api.data.events.takeMetaIdUnlessTypeIs
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
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
                val changed = event.collect(
                    onStructureChanged = { it.takeMetaIdUnlessTypeIs(LightMetaAccount.Type.PROXIED) }
                )
                val removed = event.collect(
                    onRemoved = { it.takeMetaIdUnlessTypeIs(LightMetaAccount.Type.PROXIED) }
                )

               pushNotificationsInteractor.onMetaAccountChange(changed = changed, deleted = removed)
            }.launchIn(scope)
    }
}
