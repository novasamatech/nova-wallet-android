package io.novafoundation.nova.feature_account_impl.domain.account.details

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.entropy
import io.novafoundation.nova.common.data.secrets.v2.getAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.seed
import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.domain.model.hasChainAccountIn
import io.novafoundation.nova.feature_account_api.presenatation.account.add.SecretType
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountInChain.From
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateApplicationConfig
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AccountDetailsInteractor(
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

    suspend fun getChainProjections(metaAccount: MetaAccount): GroupedList<From, AccountInChain> = withContext(Dispatchers.Default) {
        val chains = shownChainsFor(metaAccount.type)

        chains.map { chain ->
            val address = metaAccount.addressIn(chain)
            val accountId = metaAccount.accountIdIn(chain)

            val projection = if (address != null && accountId != null) {
                AccountInChain.Projection(address, accountId)
            } else {
                null
            }

            AccountInChain(
                chain = chain,
                projection = projection,
                from = if (metaAccount.hasChainAccountIn(chain.id)) From.CHAIN_ACCOUNT else From.META_ACCOUNT
            )
        }
            .sortedWith(accountInChainComparator(metaAccount.type))
            .groupBy(AccountInChain::from)
            .toSortedMap(compareBy(From::ordering))
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

    private val AccountInChain.hasChainAccount
        get() = projection != null

    private fun accountInChainComparator(metaAccountType: LightMetaAccount.Type): Comparator<AccountInChain> {
        val hasAccountOrdering: Comparator<AccountInChain> = when(metaAccountType) {
            LightMetaAccount.Type.LEDGER -> compareBy { !it.hasChainAccount }
            else -> compareBy { it.hasChainAccount }
        }

        return hasAccountOrdering.then(Chain.defaultComparatorFrom(AccountInChain::chain))
    }

    private suspend fun shownChainsFor(metaAccountType: LightMetaAccount.Type): List<Chain> {
        val allChains = chainRegistry.currentChains.first()

        return when (metaAccountType) {
            LightMetaAccount.Type.LEDGER -> {
                val ledgerSupportedChainIds = SubstrateApplicationConfig.all().mapToSet { it.chainId }

                allChains.filter { it.id in ledgerSupportedChainIds }
            }
            else -> allChains
        }
    }
}

private val From.ordering
    get() = when (this) {
        From.CHAIN_ACCOUNT -> 0
        From.META_ACCOUNT -> 1
    }
