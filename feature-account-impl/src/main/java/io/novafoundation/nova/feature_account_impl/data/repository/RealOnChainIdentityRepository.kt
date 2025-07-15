package io.novafoundation.nova.feature_account_impl.data.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.format.getAddressSchemeOrThrow
import io.novafoundation.nova.common.address.get
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.groupByToSet
import io.novafoundation.nova.common.utils.hasModule
import io.novafoundation.nova.common.utils.identity
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_account_api.data.model.AccountAddressMap
import io.novafoundation.nova.feature_account_api.data.model.AccountIdKeyMap
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_account_api.data.model.ChildIdentity
import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_impl.data.network.blockchain.bindings.bindIdentity
import io.novafoundation.nova.feature_account_impl.data.network.blockchain.bindings.bindSuperOf
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.addressScheme
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getChainOrNull
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealOnChainIdentityRepository(
    private val storageDataSource: StorageDataSource,
    private val chainRegistry: ChainRegistry
) : OnChainIdentityRepository {

    companion object {

        private val MULTICHAIN_IDENTITY_CHAINS = listOf(Chain.Geneses.POLKADOT_PEOPLE, Chain.Geneses.KUSAMA_PEOPLE)
    }

    override suspend fun getIdentitiesFromIdsHex(
        chainId: ChainId,
        accountIdsHex: Collection<String>
    ): AccountIdMap<OnChainIdentity?> = withContext(Dispatchers.Default) {
        val accountIds = accountIdsHex.map { it.fromHex() }

        getIdentitiesFromIds(accountIds, chainId).mapKeys { (accountId, _) -> accountId.value.toHexString() }
    }

    override suspend fun getIdentitiesFromIds(
        accountIds: Collection<AccountId>,
        chainId: ChainId
    ): AccountIdKeyMap<OnChainIdentity?> = withContext(Dispatchers.Default) {
        val identityChainId = findIdentityChain(chainId)
        val uniqueIds = accountIds.mapToSet(AccountId::intoKey)

        getIdentitiesFromIdOnIdentityChain(uniqueIds, identityChainId)
    }

    private suspend fun getIdentitiesFromIdOnIdentityChain(
        accountIds: Set<AccountIdKey>,
        identityChainId: ChainId
    ): AccountIdKeyMap<OnChainIdentity?> = withContext(Dispatchers.Default) {
        storageDataSource.query(identityChainId) {
            if (!runtime.metadata.hasModule(Modules.IDENTITY)) {
                return@query emptyMap()
            }

            val superOfArguments = accountIds.map { listOf(it.value) }
            val superOfValues = runtime.metadata.identity().storage("SuperOf").entries(
                keysArguments = superOfArguments,
                keyExtractor = { (accountId: AccountId) -> AccountIdKey(accountId) },
                binding = { value, _ -> bindSuperOf(value) }
            )

            val parentIdentityIds = superOfValues.values.filterNotNull().mapToSet { AccountIdKey(it.parentId) }
            val parentIdentities = fetchIdentities(parentIdentityIds)

            val childIdentities = superOfValues.filterNotNull().mapValues { (_, superOf) ->
                val parentIdentity = parentIdentities[superOf.parentId]

                parentIdentity?.let { ChildIdentity(superOf.childName, it) }
            }

            val leftAccountIds = accountIds - childIdentities.keys - parentIdentities.keys

            val rootIdentities = fetchIdentities(leftAccountIds.toList())

            rootIdentities + childIdentities + parentIdentities
        }
    }

    override suspend fun getIdentityFromId(
        chainId: ChainId,
        accountId: AccountId
    ): OnChainIdentity? = withContext(Dispatchers.Default) {
        val identityChainId = findIdentityChain(chainId)

        storageDataSource.query(identityChainId) {
            if (!runtime.metadata.hasModule(Modules.IDENTITY)) {
                return@query null
            }

            val parentRelationship = runtime.metadata.identity().storage("SuperOf").query(accountId, binding = ::bindSuperOf)

            if (parentRelationship != null) {
                val parentIdentity = fetchIdentity(parentRelationship.parentId)

                parentIdentity?.let {
                    ChildIdentity(parentRelationship.childName, parentIdentity)
                }
            } else {
                fetchIdentity(accountId)
            }
        }
    }

    override suspend fun getMultiChainIdentities(
        accountIds: Collection<AccountIdKey>
    ): AccountIdKeyMap<OnChainIdentity?> {
        val accountIdsByAddressScheme = accountIds.groupByToSet { it.getAddressSchemeOrThrow() }
        val identityChains = MULTICHAIN_IDENTITY_CHAINS.mapNotNull { chainRegistry.getChainOrNull(it) }

        return buildMap {
            identityChains.onEach { identityChain ->
                val addressScheme = identityChain.addressScheme
                val accountIdsPerScheme = accountIdsByAddressScheme[addressScheme] ?: return@onEach

                val identities = getIdentitiesFromIdOnIdentityChain(accountIdsPerScheme, identityChain.id)
                putAll(identities)

                // Early return if we already fetched all required identities
                if (size == accountIds.size) return@buildMap
            }
        }
    }

    override suspend fun getIdentitiesFromAddresses(chain: Chain, accountAddresses: List<String>): AccountAddressMap<OnChainIdentity?> {
        val accountIds = accountAddresses.map(chain::accountIdOf)

        val identitiesByAccountId = getIdentitiesFromIds(accountIds, chain.id)

        return accountAddresses.associateWith { address ->
            val accountId = chain.accountIdOf(address)

            identitiesByAccountId[accountId]
        }
    }

    private suspend fun StorageQueryContext.fetchIdentities(accountIdsHex: Collection<AccountIdKey>): AccountIdKeyMap<OnChainIdentity?> {
        return runtime.metadata.module("Identity").storage("IdentityOf").entries(
            keysArguments = accountIdsHex.map { listOf(it.value) },
            keyExtractor = { (accountId: AccountId) -> AccountIdKey(accountId) },
            binding = { value, _ -> bindIdentity(value) }
        )
    }

    private suspend fun StorageQueryContext.fetchIdentity(accountId: AccountId): OnChainIdentity? {
        return runtime.metadata.module("Identity").storage("IdentityOf")
            .query(accountId, binding = ::bindIdentity)
    }

    private suspend fun findIdentityChain(identitiesRequestedOn: ChainId): ChainId {
        val requestedChain = chainRegistry.getChain(identitiesRequestedOn)
        return findIdentityChain(requestedChain).id
    }

    private suspend fun findIdentityChain(requestedChain: Chain): Chain {
        val identityChain = requestedChain.additional?.identityChain?.let { chainRegistry.getChainOrNull(it) }
        return identityChain ?: requestedChain
    }
}
