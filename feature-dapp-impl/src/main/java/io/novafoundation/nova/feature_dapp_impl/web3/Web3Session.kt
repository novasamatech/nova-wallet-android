package io.novafoundation.nova.feature_dapp_impl.web3

import io.novafoundation.nova.feature_dapp_impl.util.UrlNormalizer
import java.util.concurrent.ConcurrentHashMap

interface Web3Session {

    enum class AuthorizationState {
        ALLOWED, REJECTED, NONE
    }

    suspend fun authorizationStateFor(url: String): AuthorizationState

    suspend fun updateAuthorizationState(url: String, state: AuthorizationState)
}

class InMemoryWeb3Session : Web3Session {

    private val storage = ConcurrentHashMap<String, Web3Session.AuthorizationState>()

    override suspend fun authorizationStateFor(url: String): Web3Session.AuthorizationState {
        return storage.getOrDefault(UrlNormalizer.normalizeUrl(url), Web3Session.AuthorizationState.NONE)
    }

    override suspend fun updateAuthorizationState(url: String, state: Web3Session.AuthorizationState) {
        storage[UrlNormalizer.normalizeUrl(url)] = state
    }
}
