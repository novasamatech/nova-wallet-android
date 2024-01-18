package io.novafoundation.nova.feature_account_api.data.repository.addAccount

interface AddAccountRepository<T> {

    suspend fun addAccount(payload: T): Long
}
