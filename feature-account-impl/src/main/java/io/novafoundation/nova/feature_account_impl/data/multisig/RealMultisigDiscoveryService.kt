package io.novafoundation.nova.feature_account_impl.data.multisig

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.MultiMapList
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.common.utils.mutableMultiListMapOf
import io.novafoundation.nova.common.utils.put
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigDiscoveryService
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.collectAddedIds
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.multisig.MultisigAddAccountRepository
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.OnChainIdentity
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdKeyIn
import io.novafoundation.nova.feature_account_impl.BuildConfig
import io.novafoundation.nova.feature_account_impl.data.multisig.model.DiscoveredMultisig
import io.novafoundation.nova.feature_account_impl.data.multisig.model.otherSignatories
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.enabledChains
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/*
TODO multisig:
1. Refactor ProxySyncService to merge proxy and multisig reachability graphs
2. Integrate MultisigSyncService to RealMetaAccountChangesEventBus
3. Move multisig-related logic to separate module similar to proxies
4. automaticSync should watch out for chain updates in the most efficient way
5. Multisig account should be created as universal/chain account depending on the signatory account type -
chain account signatory should create chain account multisig, evm signatory should create evm multisig
6. Integrate multisigs to MetaAccountsUpdatesRegistry - just adding ids to the repository isn't enough as ui only shows proxies, see
MetaAccountGroupingInteractor.updatedProxieds
7. Sync multisigs for proxies
 */
@FeatureScope
internal class RealMultisigDiscoveryService @Inject constructor(
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val multisigRepository: MultisigRepository,
    private val addMultisigRepository: MultisigAddAccountRepository,
    private val rootScope: RootScope,
    @OnChainIdentity private val identityProvider: IdentityProvider,
    private val metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry,
) : MultisigDiscoveryService {

    private val shouldSyncWatchOnlyMultisigs: Boolean = BuildConfig.DEBUG

    override fun startManualAccountDiscoverySync() {
        rootScope.launch(Dispatchers.Default) {
            startSyncInternal()
        }
    }

    override fun automaticAccountDiscoverySync(): Flow<*> {
        return flowOf {
            startSyncInternal()
        }
    }

    private suspend fun startSyncInternal() = runCatching {
        val metaAccounts = getSyncableMetaAccounts()
        if (metaAccounts.isEmpty()) return@runCatching

        val supportedProxyChains = getSupportedMultisigChains()

        Log.d(LOG_TAG, "Starting syncing multisig in ${supportedProxyChains.size} chains")

        supportedProxyChains.forEach { chain ->
            syncChainMultisigs(chain, metaAccounts)
        }
    }

    private suspend fun syncChainMultisigs(
        chain: Chain,
        allMetaAccounts: List<MetaAccount>
    ) = runCatching {
        Log.d(LOG_TAG, "Started syncing multisigs for ${chain.name}")

        val availableAccounts = chain.getAvailableMetaAccounts(allMetaAccounts)
        val availableAccountIds = availableAccounts.mapToSet { it.requireAccountIdIn(chain).intoKey() }

        val oldMultisigs = getExistingMultisigs()
        val multisigs = multisigRepository.findMultisigAccounts(chain, availableAccountIds)

        val multisigAccountIds = multisigs.map { it.accountId.value }
        val multisigsBySigner = associateMultisigsWithRequestedAccounts(multisigs, availableAccountIds)

        val identities = identityProvider.identitiesFor(multisigAccountIds, chain.id)

        val createdIds = createMetaAccounts(chain, oldMultisigs, multisigsBySigner, identities, availableAccounts)
        metaAccountsUpdatesRegistry.addMetaIds(createdIds)
        createdIds
    }.onFailure {
        Log.e(LOG_TAG, "Failed to sync multisigs in chain ${chain.name}", it)
    }.onSuccess {
        Log.d(LOG_TAG, "Finished syncing multisigs for ${chain.name}. Added ${it.size} accounts")
    }

    private fun associateMultisigsWithRequestedAccounts(
        multisigs: List<DiscoveredMultisig>,
        requestedSignatoryIds: Set<AccountIdKey>
    ): MultiMapList<AccountIdKey, DiscoveredMultisig> {
        return mutableMultiListMapOf<AccountIdKey, DiscoveredMultisig>().apply {
            multisigs.forEach { multisig ->
                multisig.allSignatories.forEach { signatory ->
                    if (signatory in requestedSignatoryIds) {
                        put(signatory, multisig)
                    }
                }
            }
        }
    }

    private suspend fun createMetaAccounts(
        chain: Chain,
        oldMultisigs: List<MultisigMetaAccount>,
        discoveredMultisigsByRequestedSigner: MultiMapList<AccountIdKey, DiscoveredMultisig>,
        identities: Map<AccountIdKey, Identity?>,
        allAvailableAccounts: List<MetaAccount>,
    ): List<Long> {
        val oldMultisigsMetaIdsByAccountId = oldMultisigs.groupBy(
            keySelector = { it.requireAccountIdKeyIn(chain) },
            valueTransform = { it.signatoryMetaId }
        )

        val accountPayloads = allAvailableAccounts.flatMap { potentialSigner ->
            val potentialSignerAccountId = potentialSigner.requireAccountIdKeyIn(chain)
            val discoveredMultisigsForSigner = discoveredMultisigsByRequestedSigner[potentialSignerAccountId].orEmpty()

            discoveredMultisigsForSigner.filter { multisig ->
                val oldMultisigsForAccountId = oldMultisigsMetaIdsByAccountId[multisig.accountId].orEmpty()
                potentialSigner.id !in oldMultisigsForAccountId
            }.map { multisig ->
                val identity = identities[multisig.accountId]
                prepareAddAccountPayload(chain, multisig, potentialSigner.id, potentialSignerAccountId, identity)
            }
        }

        return addMultisigRepository.addAccount(MultisigAddAccountRepository.Payload(accountPayloads))
            .collectAddedIds()
    }

    private fun prepareAddAccountPayload(
        chain: Chain,
        multisig: DiscoveredMultisig,
        signatoryMetaId: Long,
        signatoryAccountId: AccountIdKey,
        identity: Identity?
    ) = MultisigAddAccountRepository.AccountPayload(
        chain = chain,
        multisigAccountId = multisig.accountId,
        otherSignatories = multisig.otherSignatories(signatoryAccountId),
        threshold = multisig.threshold,
        signatoryMetaId = signatoryMetaId,
        signatoryAccountId = signatoryAccountId,
        identity = identity
    )

    private fun Chain.getAvailableMetaAccounts(metaAccounts: List<MetaAccount>): List<MetaAccount> {
        return metaAccounts.filter { metaAccount -> metaAccount.hasAccountIn(chain = this) }
    }

    private suspend fun getSupportedMultisigChains(): List<Chain> {
        return chainRegistry.enabledChains()
            .filter { multisigRepository.supportsMultisigSync(it) }
    }

    private suspend fun getSyncableMetaAccounts(): List<MetaAccount> {
        return accountRepository.getActiveMetaAccounts()
            .filter { it.isAllowedToSyncMultisig(shouldSyncWatchOnlyMultisigs) }
    }

    private suspend fun getExistingMultisigs(): List<MultisigMetaAccount> {
        return accountRepository.getActiveMetaAccounts()
            .filterIsInstance<MultisigMetaAccount>()
    }

    private fun MetaAccount.isAllowedToSyncMultisig(shouldSyncWatchOnly: Boolean): Boolean {
        return when (type) {
            LightMetaAccount.Type.SECRETS,
            LightMetaAccount.Type.PARITY_SIGNER,
            LightMetaAccount.Type.LEDGER_LEGACY,
            LightMetaAccount.Type.LEDGER,
            LightMetaAccount.Type.POLKADOT_VAULT,

            LightMetaAccount.Type.WATCH_ONLY -> shouldSyncWatchOnly

            LightMetaAccount.Type.PROXIED,
            LightMetaAccount.Type.MULTISIG -> false
        }
    }
}
