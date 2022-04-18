package io.novafoundation.nova.feature_dapp_impl.web3.session

import kotlinx.coroutines.flow.Flow

interface Web3Session {

    class Authorization(
        val state: State,
        val baseUrl: String,
        val dAppTitle: String?,
        val metaId: Long
    ) {
        enum class State {
            ALLOWED, REJECTED, NONE
        }
    }

    suspend fun authorizationStateFor(url: String, metaId: Long): Authorization.State

    suspend fun updateAuthorization(
        state: Authorization.State,
        fullUrl: String,
        dAppTitle: String,
        metaId: Long
    )

    suspend fun revokeAuthorization(
        url: String,
        metaId: Long
    )

    fun observeAuthorizationsFor(metaId: Long): Flow<List<Authorization>>
}
