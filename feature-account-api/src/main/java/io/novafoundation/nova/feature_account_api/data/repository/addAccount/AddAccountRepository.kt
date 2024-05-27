package io.novafoundation.nova.feature_account_api.data.repository.addAccount

import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount

interface AddAccountRepository<T> {

    suspend fun addAccount(payload: T): AddAccountResult
}

sealed interface AddAccountResult {

    val metaId: Long

    val type: LightMetaAccount.Type

    class AccountAdded(override val metaId: Long, override val type: LightMetaAccount.Type) : AddAccountResult

    class AccountChanged(override val metaId: Long, override val type: LightMetaAccount.Type) : AddAccountResult

    class NoOp(override val metaId: Long) : AddAccountResult
}
