package io.novafoundation.nova.feature_account_api.data.events

import io.novafoundation.nova.common.utils.bus.BaseEventBus
import io.novafoundation.nova.common.utils.bus.EventBus

class MetaAccountChangesEventBus : BaseEventBus<MetaAccountChangesEventBus.Event>() {

    sealed interface Event : EventBus.Event {

        class AccountAdded(val metaId: Long) : Event

        class AccountChanged(val metaId: Long) : Event

        class AccountRemoved(val metaId: Long) : Event
    }
}
