package io.novafoundation.nova.feature_dapp_impl.web3.session

import io.novafoundation.nova.core_db.dao.DappAuthorizationDao
import io.novafoundation.nova.core_db.model.DappAuthorizationLocal
import io.novafoundation.nova.feature_dapp_impl.util.Urls
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session.AuthorizationState

class DbWeb3Session(
    private val authorizationDao: DappAuthorizationDao
) : Web3Session {

    override suspend fun authorizationStateFor(url: String): AuthorizationState {
        val authorization = authorizationDao.getAuthorization(Urls.normalizeUrl(url))

        return mapAuthorizedFlagToAuthorizationState(authorization?.authorized)
    }

    override suspend fun updateAuthorizationState(url: String, state: AuthorizationState) {
        val authorization = DappAuthorizationLocal(
            baseUrl = Urls.normalizeUrl(url),
            authorized = mapAuthorizationStateToAuthorizedFlag(state)
        )

        authorizationDao.updateAuthorization(authorization)
    }

    private fun mapAuthorizationStateToAuthorizedFlag(
        authorizationState: AuthorizationState
    ) = when (authorizationState) {
        AuthorizationState.ALLOWED -> true
        AuthorizationState.REJECTED -> false
        AuthorizationState.NONE -> null
    }

    private fun mapAuthorizedFlagToAuthorizationState(
        authorized: Boolean?
    ) = when (authorized) {
        null -> AuthorizationState.NONE
        true -> AuthorizationState.ALLOWED
        false -> AuthorizationState.REJECTED
    }
}
