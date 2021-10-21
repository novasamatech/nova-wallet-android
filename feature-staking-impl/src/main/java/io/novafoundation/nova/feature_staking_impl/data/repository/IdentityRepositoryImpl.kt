package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.common.utils.toHexAccountId
import io.novafoundation.nova.feature_staking_api.domain.api.AccountAddressMap
import io.novafoundation.nova.feature_staking_api.domain.api.IdentityRepository
import io.novafoundation.nova.feature_staking_api.domain.model.ChildIdentity
import io.novafoundation.nova.feature_staking_api.domain.model.Identity
import io.novafoundation.nova.feature_staking_api.domain.model.SuperOf
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindIdentity
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindSuperOf
import io.novafoundation.nova.runtime.ext.hexAccountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.Type
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IdentityRepositoryImpl(
    private val bulkRetriever: BulkRetriever,
    private val chainRegistry: ChainRegistry,
) : IdentityRepository {

    override suspend fun getIdentitiesFromIds(
        chainId: ChainId,
        accountIdsHex: List<String>
    ) = withContext(Dispatchers.Default) {
        val socketService = chainRegistry.getSocket(chainId)
        val runtime = chainRegistry.getRuntime(chainId)

        val identityModule = runtime.metadata.module("Identity")

        val identityOfStorage = identityModule.storage("IdentityOf")
        val identityOfReturnType = identityOfStorage.type.value!!

        val superOfStorage = identityModule.storage("SuperOf")
        val superOfReturnType = superOfStorage.type.value!!
        val superOfKeys = superOfStorage.accountMapStorageKeys(runtime, accountIdsHex)

        val superOfValues = bulkRetriever.queryKeys(socketService, superOfKeys)
            .mapKeys { (fullKey, _) -> fullKey.accountIdFromMapKey() }
            .mapValuesNotNull { (_, value) ->
                value?.let { bindSuperOf(it, runtime, superOfReturnType) }
            }

        val parentIdentityIds = superOfValues.values.map(SuperOf::parentIdHex).distinct()
        val parentIdentityKeys = identityOfStorage.accountMapStorageKeys(runtime, parentIdentityIds)

        val parentIdentities = fetchIdentities(socketService, parentIdentityKeys, runtime, identityOfReturnType)

        val childIdentities = superOfValues.mapValues { (_, superOf) ->
            val parentIdentity = parentIdentities[superOf.parentIdHex]

            parentIdentity?.let { ChildIdentity(superOf.childName, it) }
        }

        val leftAccountIds = accountIdsHex.toSet() - childIdentities.keys - parentIdentities.keys
        val leftIdentityKeys = identityOfStorage.accountMapStorageKeys(runtime, leftAccountIds.toList())

        val rootIdentities = fetchIdentities(socketService, leftIdentityKeys, runtime, identityOfReturnType)

        rootIdentities + childIdentities + parentIdentities
    }

    override suspend fun getIdentitiesFromAddresses(chain: Chain, accountAddresses: List<String>): AccountAddressMap<Identity?> {
        val accountIds = accountAddresses.map(chain::hexAccountIdOf)

        val identitiesByAccountId = getIdentitiesFromIds(chain.id, accountIds)

        return accountAddresses.associateWith { identitiesByAccountId[it.toHexAccountId()] }
    }

    private suspend fun fetchIdentities(
        socketService: SocketService,
        keys: List<String>,
        runtime: RuntimeSnapshot,
        returnType: Type<*>
    ): Map<String, Identity?> {
        return bulkRetriever.queryKeys(socketService, keys)
            .mapKeys { (fullKey, _) -> fullKey.accountIdFromMapKey() }
            .mapValues { (_, value) ->
                value?.let { bindIdentity(it, runtime, returnType) }
            }
    }
}
