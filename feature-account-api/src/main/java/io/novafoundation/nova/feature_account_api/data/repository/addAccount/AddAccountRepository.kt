package io.novafoundation.nova.feature_account_api.data.repository.addAccount

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

    object NoOp : AddAccountResult
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
