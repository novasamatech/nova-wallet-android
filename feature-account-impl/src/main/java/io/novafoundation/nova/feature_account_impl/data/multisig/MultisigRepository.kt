package io.novafoundation.nova.feature_account_impl.data.multisig

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.fromHexOrNull
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.HexString
import io.novafoundation.nova.common.utils.RuntimeContext
import io.novafoundation.nova.common.utils.callHash
import io.novafoundation.nova.common.utils.fromHex
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.feature_account_impl.data.multisig.api.FindMultisigsApi
import io.novafoundation.nova.feature_account_impl.data.multisig.api.request.FindMultisigsRequest
import io.novafoundation.nova.feature_account_impl.data.multisig.api.request.OffChainPendingMultisigInfoRequest
import io.novafoundation.nova.feature_account_impl.data.multisig.api.response.FindMultisigsResponse
import io.novafoundation.nova.feature_account_impl.data.multisig.api.response.GetPedingMultisigOperationsResponse
import io.novafoundation.nova.feature_account_impl.data.multisig.api.response.GetPedingMultisigOperationsResponse.OperationRemote
import io.novafoundation.nova.feature_account_impl.data.multisig.api.response.MultisigRemote
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.model.OnChainMultisig
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.multisig
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.multisigs
import io.novafoundation.nova.feature_account_impl.data.multisig.model.DiscoveredMultisig
import io.novafoundation.nova.feature_account_impl.data.multisig.model.OffChainPendingMultisigOperationInfo
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface MultisigRepository {

    fun supportsMultisigSync(chain: Chain): Boolean

    suspend fun findMultisigAccounts(chain: Chain, accountIds: Set<AccountIdKey>): List<DiscoveredMultisig>

    suspend fun getPendingOperationIds(chain: Chain, accountIdKey: AccountIdKey): Set<CallHash>

    suspend fun subscribePendingOperations(
        chain: Chain,
        accountIdKey: AccountIdKey,
        operationIds: Collection<CallHash>
    ): Flow<Map<CallHash, OnChainMultisig?>>

    suspend fun getOffChainPendingOperationsInfo(
        chain: Chain,
        accountId: AccountIdKey,
        pendingCallHashes: Collection<CallHash>
    ): Result<Map<CallHash, OffChainPendingMultisigOperationInfo>>
}

@FeatureScope
class RealMultisigRepository @Inject constructor(
    private val api: FindMultisigsApi,
    @Named(REMOTE_STORAGE_SOURCE)
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
) : MultisigRepository {

    override fun supportsMultisigSync(chain: Chain): Boolean {
        return chain.hasMultisigApi()
    }

    override suspend fun findMultisigAccounts(chain: Chain, accountIds: Set<AccountIdKey>): List<DiscoveredMultisig> {
        val apiConfig = chain.multisigApi() ?: return emptyList()
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

    override suspend fun getOffChainPendingOperationsInfo(
        chain: Chain,
        accountId: AccountIdKey,
        pendingCallHashes: Collection<CallHash>
    ): Result<Map<CallHash, OffChainPendingMultisigOperationInfo>> {
        val apiConfig = chain.multisigApi() ?: return Result.success(emptyMap())

        return kotlin.runCatching {
            val request = OffChainPendingMultisigInfoRequest(accountId, pendingCallHashes)
            val response = api.getCallDatas(apiConfig.url, request)
            response.toDomain(chain)
        }
            .onFailure { Log.e("RealMultisigRepository", "Failed to fetch call datas in ${chain.name}", it) }
    }

    private suspend fun SubQueryResponse<GetPedingMultisigOperationsResponse>.toDomain(chain: Chain): Map<CallHash, OffChainPendingMultisigOperationInfo> {
        return chainRegistry.withRuntime(chain.id) {
            data.multisigOperations.nodes.mapNotNull { multisigOperation ->
                val callHash = CallHash.fromHexOrNull(multisigOperation.callHash) ?: return@mapNotNull null
                val callData = parseCallData(multisigOperation.callData, callHash, chain)

                OffChainPendingMultisigOperationInfo(
                    timestamp = multisigOperation.timestamp(),
                    callData = callData,
                    callHash = callHash
                )
            }
                .associateBy { it.callHash }
        }
    }

    private fun OperationRemote.timestamp(): Duration {
        val inSeconds = events.nodes.firstOrNull()?.timestamp ?: timestamp
        return inSeconds.seconds
    }

    context(RuntimeContext)
    private fun parseCallData(
        callData: HexString?,
        callHash: CallHash,
        chain: Chain
    ): GenericCall.Instance? {
        if (callData == null) return null

        return runCatching {
            val hashFromCallData = callData.callHash().intoKey()
            require(hashFromCallData == callHash) {
                "Call-data does not match call hash. Expected hash: $callHash, Actual hash: $hashFromCallData. Call data: $callData"
            }

            GenericCall.fromHex(callData)
        }
            .onFailure { Log.e("RealMultisigRepository", "Failed to decode call data on ${chain.name}: $callData}", it) }
            .getOrNull()
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

    private fun Chain.hasMultisigApi(): Boolean {
        return hasExternalApi<ExternalApi.Multisig>()
    }

    private fun Chain.multisigApi(): ExternalApi.Multisig? {
        return externalApi()
    }
}
