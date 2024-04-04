package io.novafoundation.nova.feature_account_api.data.repository.addAccount

interface AddAccountRepository<T> {

    suspend fun addAccount(payload: T): AddAccountResult
}

sealed interface AddAccountResult {

    val metaIds: List<Long>

    class AccountAdded(override val metaIds: List<Long>) : AddAccountResult {
        constructor(metaId: Long) : this(listOf(metaId))
    }

    class AccountChanged(override val metaIds: List<Long>) : AddAccountResult {
        constructor(metaId: Long) : this(listOf(metaId))
    }
}
