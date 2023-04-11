package io.novafoundation.nova.feature_dapp_impl.domain.authorizedDApps

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session.Authorization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class AuthorizedDAppsInteractor(
    private val accountRepository: AccountRepository,
    private val metadataRepository: DAppMetadataRepository,
    private val web3Session: Web3Session
) {

    suspend fun revokeAuthorization(url: String) {
        val currentAccount = accountRepository.getSelectedMetaAccount()

        web3Session.revokeAuthorization(url, currentAccount.id)
    }

    fun observeAuthorizedDApps(): Flow<List<AuthorizedDApp>> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            val catalog = metadataRepository.getDAppCatalog()
            val dApps = catalog.dApps.associateBy(DappMetadata::baseUrl)

            web3Session.observeAuthorizationsFor(metaAccount.id)
                .map { authorizations -> authorizations.filter { it.state == Authorization.State.ALLOWED } }
                .mapList { authorization ->
                    val metadata = dApps[authorization.baseUrl]

                    AuthorizedDApp(
                        baseUrl = authorization.baseUrl,
                        name = metadata?.name ?: authorization.dAppTitle,
                        iconLink = metadata?.iconLink
                    )
                }
        }
    }
}
