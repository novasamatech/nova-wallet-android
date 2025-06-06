package io.novafoundation.nova.feature_account_api.data.repository.addAccount

import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus.Event
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount

interface AddAccountRepository<T> {

    suspend fun addAccount(payload: T): AddAccountResult
}

sealed interface AddAccountResult {

    sealed interface HadEffect : AddAccountResult

    interface SingleAccountChange {

        val metaId: Long
    }

    class AccountAdded(override val metaId: Long, val type: LightMetaAccount.Type) : HadEffect, SingleAccountChange

    class AccountChanged(override val metaId: Long, val type: LightMetaAccount.Type) : HadEffect, SingleAccountChange

    class Batch(val updates: List<HadEffect>) : HadEffect

    data object NoOp : AddAccountResult
}

fun AddAccountResult.toAccountBusEvent(): Event? {
    return when (this) {
        is AddAccountResult.HadEffect -> toAccountBusEvent()
        is AddAccountResult.NoOp -> null
    }
}

fun AddAccountResult.HadEffect.toAccountBusEvent(): Event {
    return when (this) {
        is AddAccountResult.AccountAdded -> Event.AccountAdded(metaId, type)
        is AddAccountResult.AccountChanged -> Event.AccountStructureChanged(metaId, type)
        is AddAccountResult.Batch -> Event.BatchUpdate(updates.map { it.toAccountBusEvent() })
    }
}

suspend fun <T> AddAccountRepository<T>.addAccountWithSingleChange(payload: T): AddAccountResult.SingleAccountChange {
    val result = addAccount(payload)
    require(result is AddAccountResult.SingleAccountChange)

    return result
}

fun List<AddAccountResult>.batchIfNeeded(): AddAccountResult {
    val updatesThatHadEffect = filterIsInstance<AddAccountResult.HadEffect>()

    return when (updatesThatHadEffect.size) {
        0 -> AddAccountResult.NoOp
        1 -> updatesThatHadEffect.single()
        else -> AddAccountResult.Batch(updatesThatHadEffect)
    }
}

fun AddAccountResult.visit(
    onAdd: (AddAccountResult.AccountAdded) -> Unit
) {
    when (this) {
        is AddAccountResult.AccountAdded -> onAdd(this)
        is AddAccountResult.AccountChanged -> Unit
        is AddAccountResult.Batch -> updates.onEach { it.visit(onAdd) }
        AddAccountResult.NoOp -> Unit
    }
}

fun AddAccountResult.collectAddedIds(): List<Long> {
    return buildList {
        visit {
            add(it.metaId)
        }
    }
}
