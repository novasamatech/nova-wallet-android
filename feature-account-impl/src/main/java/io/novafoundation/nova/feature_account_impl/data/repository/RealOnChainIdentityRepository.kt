package io.novafoundation.nova.feature_account_impl.data.repository

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.hasModule
import io.novafoundation.nova.common.utils.identity
import io.novafoundation.nova.common.utils.toHexAccountId
import io.novafoundation.nova.feature_account_api.data.model.AccountAddressMap
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_account_api.data.model.ChildIdentity
import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.feature_account_api.data.model.SuperOf
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_impl.data.network.blockchain.bindings.bindIdentity
import io.novafoundation.nova.feature_account_impl.data.network.blockchain.bindings.bindSuperOf
import io.novafoundation.nova.runtime.ext.hexAccountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealOnChainIdentityRepository(
    private val storageDataSource: StorageDataSource,
) : OnChainIdentityRepository {

    override suspend fun getIdentitiesFromIds(
        chainId: ChainId,
        accountIdsHex: Collection<String>
    ): AccountIdMap<OnChainIdentity?> = withContext(Dispatchers.Default) {
        storageDataSource.query(chainId) {
            if (!runtime.metadata.hasModule(Modules.IDENTITY)) {
                return@query emptyMap()
            }

            val superOfArguments = accountIdsHex.map { listOf(it.fromHex()) }
            val superOfValues = runtime.metadata.identity().storage("SuperOf").entries(
                keysArguments = superOfArguments,
                keyExtractor = { (accountId: AccountId) -> accountId.toHexString() },
                binding = { value, _ -> bindSuperOf(value) }
            )

            val parentIdentityIds = superOfValues.values.filterNotNull().map(SuperOf::parentIdHex).distinct()
            val parentIdentities = fetchIdentities(parentIdentityIds)

            val childIdentities = superOfValues.filterNotNull().mapValues { (_, superOf) ->
                val parentIdentity = parentIdentities[superOf.parentIdHex]

                parentIdentity?.let { ChildIdentity(superOf.childName, it) }
            }

            val leftAccountIds = accountIdsHex.toSet() - childIdentities.keys - parentIdentities.keys

            val rootIdentities = fetchIdentities(leftAccountIds.toList())

            rootIdentities + childIdentities + parentIdentities
        }
    }

    override suspend fun getIdentityFromId(
        chainId: ChainId,
        accountId: AccountId
    ): OnChainIdentity? = withContext(Dispatchers.Default) {
        storageDataSource.query(chainId) {
            if (!runtime.metadata.hasModule(Modules.IDENTITY)) {
                return@query null
            }

            val parentRelationship = runtime.metadata.identity().storage("SuperOf").query(accountId, binding = ::bindSuperOf)

            if (parentRelationship != null) {
                val parentIdentity = fetchIdentity(parentRelationship.parentIdHex.fromHex())

                parentIdentity?.let {
                    ChildIdentity(parentRelationship.childName, parentIdentity)
                }
            } else {
                fetchIdentity(accountId)
            }
        }
    }

    override suspend fun getIdentitiesFromAddresses(chain: Chain, accountAddresses: List<String>): AccountAddressMap<OnChainIdentity?> {
        val accountIds = accountAddresses.map(chain::hexAccountIdOf)

        val identitiesByAccountId = getIdentitiesFromIds(chain.id, accountIds)

        return accountAddresses.associateWith { identitiesByAccountId[it.toHexAccountId()] }
    }

    private suspend fun StorageQueryContext.fetchIdentities(accountIdsHex: List<String>): Map<String, OnChainIdentity?> {
        return runtime.metadata.module("Identity").storage("IdentityOf").entries(
            keysArguments = accountIdsHex.map { listOf(it.fromHex()) },
            keyExtractor = { (accountId: AccountId) -> accountId.toHexString() },
            binding = { value, _ -> bindIdentity(value) }
        )
    }

    private suspend fun StorageQueryContext.fetchIdentity(accountId: AccountId): OnChainIdentity? {
        return runtime.metadata.module("Identity").storage("IdentityOf")
            .query(accountId, binding = ::bindIdentity)
    }
}
