package io.novafoundation.nova.feature_account_api.data.repository.addAccount

interface AddAccountRepository<T> {

    suspend fun addAccount(payload: T): AddAccountResult
}

sealed interface AddAccountResult {

    val metaId: Long

    class AccountAdded(override val metaId: Long) : AddAccountResult

    class AccountChanged(override val metaId: Long) : AddAccountResult
}
