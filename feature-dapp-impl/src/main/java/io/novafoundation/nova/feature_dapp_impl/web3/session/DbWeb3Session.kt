package io.novafoundation.nova.feature_dapp_impl.web3.session

import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.DappAuthorizationDao
import io.novafoundation.nova.core_db.model.DappAuthorizationLocal
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session.Authorization.State
import kotlinx.coroutines.flow.Flow

class DbWeb3Session(
    private val authorizationDao: DappAuthorizationDao
) : Web3Session {

    override suspend fun authorizationStateFor(url: String, metaId: Long): State {
        val authorization = authorizationDao.getAuthorization(Urls.normalizeUrl(url), metaId)

        return mapAuthorizedFlagToAuthorizationState(authorization?.authorized)
    }

    override suspend fun updateAuthorization(state: State, fullUrl: String, dAppTitle: String, metaId: Long) {
        val authorization = DappAuthorizationLocal(
            baseUrl = Urls.normalizeUrl(fullUrl),
            authorized = mapAuthorizationStateToAuthorizedFlag(state),
            dAppTitle = dAppTitle,
            metaId = metaId
        )

        authorizationDao.updateAuthorization(authorization)
    }

    override suspend fun revokeAuthorization(url: String, metaId: Long) {
        authorizationDao.removeAuthorization(baseUrl = Urls.normalizeUrl(url), metaId)
    }

    override fun observeAuthorizationsFor(metaId: Long): Flow<List<Web3Session.Authorization>> {
        return authorizationDao.observeAuthorizations(metaId)
            .mapList(::mapAuthorizationFromLocal)
    }

    private fun mapAuthorizationFromLocal(authorization: DappAuthorizationLocal): Web3Session.Authorization {
        return Web3Session.Authorization(
            baseUrl = authorization.baseUrl,
            metaId = authorization.metaId,
            dAppTitle = authorization.dAppTitle,
            state = mapAuthorizedFlagToAuthorizationState(authorization.authorized)
        )
    }

    private fun mapAuthorizationStateToAuthorizedFlag(
        state: State
    ) = when (state) {
        State.ALLOWED -> true
        State.REJECTED -> false
        State.NONE -> null
    }

    private fun mapAuthorizedFlagToAuthorizationState(
        authorized: Boolean?
    ) = when (authorized) {
        null -> State.NONE
        true -> State.ALLOWED
        false -> State.REJECTED
    }
}
