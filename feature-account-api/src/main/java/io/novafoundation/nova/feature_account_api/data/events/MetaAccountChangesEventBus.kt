package io.novafoundation.nova.feature_account_api.data.events

import io.novafoundation.nova.common.utils.bus.EventBus
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus.Event
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount

interface MetaAccountChangesEventBus : EventBus<Event> {

    sealed interface Event : EventBus.Event {

        data class BatchUpdate(val updates: Collection<Event>) : Event

        data class AccountAdded(override val metaId: Long, override val metaAccountType: LightMetaAccount.Type) : Event, SingleUpdateEvent

        data class AccountStructureChanged(override val metaId: Long, override val metaAccountType: LightMetaAccount.Type) : Event, SingleUpdateEvent

        data class AccountRemoved(override val metaId: Long, override val metaAccountType: LightMetaAccount.Type) : Event, SingleUpdateEvent

        data class AccountNameChanged(override val metaId: Long, override val metaAccountType: LightMetaAccount.Type) : Event, SingleUpdateEvent
    }

    interface SingleUpdateEvent {

        val metaId: Long

        val metaAccountType: LightMetaAccount.Type
    }

    interface EventVisitor {

        fun visitAccountAdded(added: Event.AccountAdded) {}

        fun visitAccountStructureChanged(structureChanged: Event.AccountStructureChanged) {}

        fun visitAccountNameChanged(accountNameChanged: Event.AccountNameChanged) {}

        fun visitAccountRemoved(accountRemoved: Event.AccountRemoved) {}
    }
}

inline fun buildChangesEvent(builder: MutableList<Event>.() -> Unit): Event? {
    val allEvents = buildList(builder)
    return allEvents.combineBusEvents()
}

fun List<Event>.combineBusEvents(): Event? {
    return when (size) {
        0 -> null
        1 -> single()
        else -> Event.BatchUpdate(this)
    }
}

fun Event.visit(visitor: MetaAccountChangesEventBus.EventVisitor) {
    when (this) {
        is Event.AccountAdded -> visitor.visitAccountAdded(this)
        is Event.AccountNameChanged -> visitor.visitAccountNameChanged(this)
        is Event.AccountRemoved -> visitor.visitAccountRemoved(this)
        is Event.AccountStructureChanged -> visitor.visitAccountStructureChanged(this)
        is Event.BatchUpdate -> updates.onEach { it.visit(visitor) }
    }
}

typealias EventBusEventCollator<E, T> = (E) -> T?

fun <T> Event.collect(
    onAdd: EventBusEventCollator<Event.AccountAdded, T>? = null,
    onStructureChanged: EventBusEventCollator<Event.AccountStructureChanged, T>? = null,
    onNameChanged: EventBusEventCollator<Event.AccountNameChanged, T>? = null,
    onRemoved: EventBusEventCollator<Event.AccountRemoved, T>? = null,
): List<T> {
    val result = mutableListOf<T>()

    visit(object : MetaAccountChangesEventBus.EventVisitor {
        override fun visitAccountAdded(added: Event.AccountAdded) {
            onAdd?.invoke(added)?.let(result::add)
        }

        override fun visitAccountStructureChanged(structureChanged: Event.AccountStructureChanged) {
            onStructureChanged?.invoke(structureChanged)?.let(result::add)
        }

        override fun visitAccountNameChanged(accountNameChanged: Event.AccountNameChanged) {
            onNameChanged?.invoke(accountNameChanged)?.let(result::add)
        }

        override fun visitAccountRemoved(accountRemoved: Event.AccountRemoved) {
            onRemoved?.invoke(accountRemoved)?.let(result::add)
        }
    })

    return result
}

typealias SingleAccountEventVisitor<T> = (T) -> Unit

fun Event.visit(
    onAdd: SingleAccountEventVisitor<Event.AccountAdded>? = null,
    onStructureChanged: SingleAccountEventVisitor<Event.AccountStructureChanged>? = null,
    onNameChanged: SingleAccountEventVisitor<Event.AccountNameChanged>? = null,
    onRemoved: SingleAccountEventVisitor<Event.AccountRemoved>? = null,
) {
    visit(object : MetaAccountChangesEventBus.EventVisitor {
        override fun visitAccountAdded(added: Event.AccountAdded) {
            onAdd?.invoke(added)
        }

        override fun visitAccountStructureChanged(structureChanged: Event.AccountStructureChanged) {
            onStructureChanged?.invoke(structureChanged)
        }

        override fun visitAccountNameChanged(accountNameChanged: Event.AccountNameChanged) {
            onNameChanged?.invoke(accountNameChanged)
        }

        override fun visitAccountRemoved(accountRemoved: Event.AccountRemoved) {
            onRemoved?.invoke(accountRemoved)
        }
    })
}

fun Event.checkIncludes(
    checkAdd: Boolean = false,
    checkStructureChange: Boolean = false,
    checkNameChange: Boolean = false,
    checkAccountRemoved: Boolean = false
): Boolean {
    var includes = false
    val updateClosure: SingleAccountEventVisitor<Any> = {
        includes = true
    }

    visit(
        onAdd = updateClosure.takeIf { checkAdd },
        onStructureChanged = updateClosure.takeIf { checkStructureChange },
        onNameChanged = updateClosure.takeIf { checkNameChange },
        onRemoved = updateClosure.takeIf { checkAccountRemoved }
    )

    return includes
}

fun Event.allAffectedMetaAccountTypes(): List<LightMetaAccount.Type> {
    return collect(
        onAdd = { it.metaAccountType },
        onRemoved = { it.metaAccountType },
        onNameChanged = { it.metaAccountType },
        onStructureChanged = { it.metaAccountType }
    )
}
