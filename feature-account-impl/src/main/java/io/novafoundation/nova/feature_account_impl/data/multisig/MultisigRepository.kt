package io.novafoundation.nova.feature_account_impl.data.multisig

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.fromHexOrNull
import io.novafoundation.nova.common.address.fromHexOrThrow
import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.fromHex
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.feature_account_impl.data.multisig.api.FindMultisigsApi
import io.novafoundation.nova.feature_account_impl.data.multisig.api.request.FindMultisigsRequest
import io.novafoundation.nova.feature_account_impl.data.multisig.api.request.GetCallDatasRequest
import io.novafoundation.nova.feature_account_impl.data.multisig.api.response.FindMultisigsResponse
import io.novafoundation.nova.feature_account_impl.data.multisig.api.response.GetCallDatasResponse
import io.novafoundation.nova.feature_account_impl.data.multisig.api.response.MultisigRemote
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.model.OnChainMultisig
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.multisig
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.multisigs
import io.novafoundation.nova.feature_account_impl.data.multisig.model.DiscoveredMultisig
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ext.externalApi
import io.novafoundation.nova.runtime.ext.hasExternalApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi
import io.novafoundation.nova.runtime.multiNetwork.withRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

interface MultisigRepository {

    fun supportsMultisigSync(chain: Chain): Boolean

    suspend fun findMultisigAccounts(chain: Chain, accountIds: Set<AccountIdKey>): List<DiscoveredMultisig>

    suspend fun getPendingOperationIds(chain: Chain, accountIdKey: AccountIdKey): Set<CallHash>

    suspend fun subscribePendingOperations(
        chain: Chain,
        accountIdKey: AccountIdKey,
        operationIds: Collection<CallHash>
    ): Flow<Map<CallHash, OnChainMultisig?>>

    /**
     * Returns the call datas corresponding to the given [callHashes]. The result map may contain null when it is not possible
     * to resolve call data for corresponding call hash
     */
    suspend fun getCallDatas(chain: Chain, callHashes: List<CallHash>): Map<CallHash, GenericCall.Instance?>
}

@FeatureScope
class RealMultisigRepository @Inject constructor(
    private val api: FindMultisigsApi,
    @Named(REMOTE_STORAGE_SOURCE)
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
) : MultisigRepository {

    override fun supportsMultisigSync(chain: Chain): Boolean {
        return chain.hasExternalApi<ExternalApi.Multisig>()
    }

    override suspend fun findMultisigAccounts(chain: Chain, accountIds: Set<AccountIdKey>): List<DiscoveredMultisig> {
        val apiConfig = chain.externalApi<ExternalApi.Multisig>() ?: return emptyList()
        val request = FindMultisigsRequest(accountIds)
        return api.findMultisigs(apiConfig.url, request).toDiscoveredMultisigs()
    }

    override suspend fun getPendingOperationIds(chain: Chain, accountIdKey: AccountIdKey): Set<CallHash> {
        return remoteStorageSource.query(chain.id) {
            runtime.metadata.multisig.multisigs.keys(accountIdKey)
                .mapToSet { it.second }
        }
    }

    override suspend fun subscribePendingOperations(
        chain: Chain,
        accountIdKey: AccountIdKey,
        operationIds: Collection<CallHash>
    ): Flow<Map<CallHash, OnChainMultisig?>> {
        return remoteStorageSource.subscribeBatched(chain.id) {
            val allKeys = operationIds.map { accountIdKey to it }

            runtime.metadata.multisig.multisigs.observe(allKeys).map { operationsByKeys ->
                operationsByKeys.mapKeys { (key, _) -> key.second }
            }
        }
    }

    override suspend fun getCallDatas(
        chain: Chain,
        callHashes: List<CallHash>
    ): Map<CallHash, GenericCall.Instance?> {
        val apiConfig = chain.externalApi<ExternalApi.Multisig>() ?: return callHashes.associateWith { null }
        return kotlin.runCatching {
            val request = GetCallDatasRequest(callHashes)
            val response = api.getCallDatas(apiConfig.url, request)
            response.toCallDataMap(chain, callHashes)
        }
            .onFailure { Log.e("RealMultisigRepository", "Failed to fetch call datas in ${chain.name}", it) }
            .getOrDefault(callHashes.associateWith { null })
    }

    private suspend fun SubQueryResponse<GetCallDatasResponse>.toCallDataMap(
        chain: Chain,
        callHashes: List<CallHash>
    ): Map<CallHash, GenericCall.Instance?> {
        val compactMap = chainRegistry.withRuntime(chain.id) {
            data.multisigOperations.nodes.mapNotNull { multisigOperation ->
                runCatching {
                    val callHash = CallHash.fromHexOrThrow(multisigOperation.callHash)
                    val callData = multisigOperation.callData ?: return@mapNotNull null
                    val call = GenericCall.fromHex(callData)

                    callHash to call
                }
                    .onFailure { Log.e("RealMultisigRepository", "Failed to decode call data on ${chain.name}: ${multisigOperation.callData}", it) }
                    .getOrNull()
            }.toMap()
        }

        return callHashes.associateWith { compactMap[it] }
    }

    private fun SubQueryResponse<FindMultisigsResponse>.toDiscoveredMultisigs(): List<DiscoveredMultisig> {
        return data.accounts.nodes.mapNotNull { multisigNode ->
            DiscoveredMultisig(
                accountId = AccountIdKey.fromHexOrNull(multisigNode.id) ?: return@mapNotNull null,
                threshold = multisigNode.thresholdIfValid() ?: return@mapNotNull null,
                allSignatories = multisigNode.signatories.nodes.map { signatoryNode ->
                    AccountIdKey.fromHexOrNull(signatoryNode.signatory.id) ?: return@mapNotNull null
                }
            )
        }
    }

    private fun MultisigRemote.thresholdIfValid(): Int? {
        // TODO there is a but on SubQuery that results in threshold=0 for threshold 1 multisigs
        return threshold.takeIf { it >= 1 }
    }
}
