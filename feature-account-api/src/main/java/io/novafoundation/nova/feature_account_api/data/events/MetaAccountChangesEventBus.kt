package io.novafoundation.nova.feature_account_api.data.events

import io.novafoundation.nova.common.utils.bus.EventBus
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount

interface MetaAccountChangesEventBus: EventBus<MetaAccountChangesEventBus.Event> {

    sealed interface Event : EventBus.Event {

        val metaId: Long

        val metaAccountType: LightMetaAccount.Type

        class AccountAdded(override val metaId: Long, override val metaAccountType: LightMetaAccount.Type) : Event

        class AccountStructureChanged(override val metaId: Long, override val metaAccountType: LightMetaAccount.Type) : Event

        class AccountRemoved(override val metaId: Long, override val metaAccountType: LightMetaAccount.Type) : Event

        class AccountNameChanged(override val metaId: Long, override val metaAccountType: LightMetaAccount.Type): Event
    }
}
