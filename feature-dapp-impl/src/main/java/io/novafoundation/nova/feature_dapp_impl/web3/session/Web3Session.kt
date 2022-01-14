package io.novafoundation.nova.feature_dapp_impl.web3.session

interface Web3Session {

    enum class AuthorizationState {
        ALLOWED, REJECTED, NONE
    }

    suspend fun authorizationStateFor(url: String): AuthorizationState

    suspend fun updateAuthorizationState(url: String, state: AuthorizationState)
}
