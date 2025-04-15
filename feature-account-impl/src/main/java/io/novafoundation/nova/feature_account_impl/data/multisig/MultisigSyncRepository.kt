package io.novafoundation.nova.feature_account_impl.data.multisig

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.fromHexOrNull
import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_impl.data.multisig.api.FindMultisigsApi
import io.novafoundation.nova.feature_account_impl.data.multisig.api.request.FindMultisigsRequest
import io.novafoundation.nova.feature_account_impl.data.multisig.api.response.FindMultisigsResponse
import io.novafoundation.nova.feature_account_impl.data.multisig.model.DiscoveredMultisig
import io.novafoundation.nova.runtime.ext.externalApi
import io.novafoundation.nova.runtime.ext.hasExternalApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi
import javax.inject.Inject

interface MultisigSyncRepository {

    fun supportsMultisigSync(chain: Chain): Boolean

    suspend fun finsMultisigAccounts(chain: Chain, accountIds: Set<AccountIdKey>): List<DiscoveredMultisig>
}

@FeatureScope
class RealMultisigSyncRepository @Inject constructor(
    private val api: FindMultisigsApi
) : MultisigSyncRepository {
    override fun supportsMultisigSync(chain: Chain): Boolean {
        return chain.hasExternalApi<ExternalApi.Multisig>()
    }

    override suspend fun finsMultisigAccounts(chain: Chain, accountIds: Set<AccountIdKey>): List<DiscoveredMultisig> {
        val apiConfig = chain.externalApi<ExternalApi.Multisig>() ?: return emptyList()
        val request = FindMultisigsRequest(accountIds)
        return api.findMultisigs(apiConfig.url, request).toDiscoveredMultisigs()
    }

    private fun SubQueryResponse<FindMultisigsResponse>.toDiscoveredMultisigs(): List<DiscoveredMultisig> {
        return data.accounts.nodes.mapNotNull { multisigNode ->
            DiscoveredMultisig(
                accountId = AccountIdKey.fromHexOrNull(multisigNode.id) ?: return@mapNotNull null,
                threshold = multisigNode.threshold,
                allSignatories = multisigNode.signatories.nodes.map { signatoryNode ->
                    AccountIdKey.fromHexOrNull(signatoryNode.signatory.id) ?: return@mapNotNull null
                }
            )
        }
    }
}
