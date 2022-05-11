package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.hasModule
import io.novafoundation.nova.common.utils.identity
import io.novafoundation.nova.common.utils.toHexAccountId
import io.novafoundation.nova.feature_staking_api.domain.api.AccountAddressMap
import io.novafoundation.nova.feature_staking_api.domain.api.IdentityRepository
import io.novafoundation.nova.feature_staking_api.domain.model.ChildIdentity
import io.novafoundation.nova.feature_staking_api.domain.model.Identity
import io.novafoundation.nova.feature_staking_api.domain.model.SuperOf
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindIdentity
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindSuperOf
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

class IdentityRepositoryImpl(
    private val storageDataSource: StorageDataSource,
) : IdentityRepository {

    override suspend fun getIdentitiesFromIds(
        chainId: ChainId,
        accountIdsHex: List<String>
    ) = withContext(Dispatchers.Default) {
        storageDataSource.query(chainId) {
            if (!runtime.metadata.hasModule(Modules.IDENTITY)) {
                return@query emptyMap()
            }

            val superOfArguments = accountIdsHex.map { listOf(it.fromHex()) }
            val superOfValues = runtime.metadata.identity().storage("SuperOf").entries(
                keysArguments = superOfArguments,
                keyExtractor = { (accountId: AccountId) -> accountId.toHexString() },
                binding = { value, _ -> value?.let { bindSuperOf(it) } }
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

    override suspend fun getIdentitiesFromAddresses(chain: Chain, accountAddresses: List<String>): AccountAddressMap<Identity?> {
        val accountIds = accountAddresses.map(chain::hexAccountIdOf)

        val identitiesByAccountId = getIdentitiesFromIds(chain.id, accountIds)

        return accountAddresses.associateWith { identitiesByAccountId[it.toHexAccountId()] }
    }

    private suspend fun StorageQueryContext.fetchIdentities(accountIdsHex: List<String>): Map<String, Identity?> {
        return runtime.metadata.module("Identity").storage("IdentityOf").entries(
            keysArguments = accountIdsHex.map { listOf(it.fromHex()) },
            keyExtractor = { (accountId: AccountId) -> accountId.toHexString() },
            binding = { value, _ -> value?.let { bindIdentity(it) } }
        )
    }
}
