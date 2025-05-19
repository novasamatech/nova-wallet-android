package io.novafoundation.nova.feature_account_impl.domain.account.details

import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.entropy
import io.novafoundation.nova.common.data.secrets.v2.getAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.seed
import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.domain.model.hasChainAccountIn
import io.novafoundation.nova.feature_account_api.presenatation.account.add.SecretType
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountInChain.From
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.hasChainAccount
import io.novafoundation.nova.runtime.ext.addressScheme
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.enabledChains
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WalletDetailsInteractor(
    private val accountRepository: AccountRepository,
    private val secretStoreV2: SecretStoreV2,
    private val chainRegistry: ChainRegistry,
) {

    suspend fun getMetaAccount(metaId: Long): MetaAccount {
        return accountRepository.getMetaAccount(metaId)
    }

    suspend fun updateName(metaId: Long, newName: String) {
        accountRepository.updateMetaAccountName(metaId, newName)
    }

    fun chainProjectionsByAddressSchemeFlow(
        metaId: Long,
        chains: List<Chain>,
        sorting: Comparator<AccountInChain>
    ): Flow<GroupedList<AddressScheme, AccountInChain>> {
        return accountRepository.metaAccountFlow(metaId)
            .map { metaAccount ->
                chains.groupBy(Chain::addressScheme)
                    .mapValues { (_, chains) ->
                        chains.map { chain -> createAccountInChain(chain, metaAccount) }
                            .sortedWith(sorting)
                    }
                    .sortGroupsByMissingAccounts()
            }
    }

    fun chainProjectionsFlow(
        metaId: Long,
        chains: List<Chain>,
        sorting: Comparator<AccountInChain>
    ): Flow<GroupedList<From, AccountInChain>> {
        return accountRepository.metaAccountFlow(metaId)
            .map { metaAccount ->
                chains.map { chain ->
                    val from = if (metaAccount.hasChainAccountIn(chain.id)) From.CHAIN_ACCOUNT else From.META_ACCOUNT
                    val accountInChain = createAccountInChain(chain, metaAccount)

                    from to accountInChain
                }
                    .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                    .mapValues { (_, chainAccounts) -> chainAccounts.sortedWith(sorting) }
                    .toSortedMap(compareBy(From::ordering))
            }
    }

    suspend fun availableExportTypes(
        metaAccount: MetaAccount,
        chain: Chain,
    ): Set<SecretType> = withContext(Dispatchers.Default) {
        val accountId = metaAccount.accountIdIn(chain) ?: return@withContext emptySet()

        val secrets = secretStoreV2.getAccountSecrets(metaAccount.id, accountId)

        setOfNotNull(
            SecretType.MNEMONIC.takeIf { secrets.entropy() != null },
            SecretType.SEED.takeIf { secrets.seed() != null },
            SecretType.JSON // always available
        )
    }

    suspend fun getAllChains(): List<Chain> {
        return chainRegistry.enabledChains()
    }

    private fun GroupedList<AddressScheme, AccountInChain>.sortGroupsByMissingAccounts(): GroupedList<AddressScheme, AccountInChain> {
        val hasAccountsByGroup = mapValues { (_, accounts) -> accounts.any { it.hasChainAccount } }

        val comparator = compareBy<AddressScheme> { hasAccountsByGroup.getValue(it) }
            .thenBy { it.defaultOrdering }

        return toSortedMap(comparator)
    }

    private fun createAccountInChain(chain: Chain, metaAccount: MetaAccount): AccountInChain {
        val address = metaAccount.addressIn(chain)
        val accountId = metaAccount.accountIdIn(chain)

        val projection = if (address != null && accountId != null) {
            AccountInChain.Projection(address, accountId)
        } else {
            null
        }

        return AccountInChain(chain = chain, projection = projection)
    }
}

private val From.ordering
    get() = when (this) {
        From.CHAIN_ACCOUNT -> 0
        From.META_ACCOUNT -> 1
    }

private val AddressScheme.defaultOrdering
    get() = when(this) {
        AddressScheme.SUBSTRATE -> 0
        AddressScheme.EVM -> 1
    }
