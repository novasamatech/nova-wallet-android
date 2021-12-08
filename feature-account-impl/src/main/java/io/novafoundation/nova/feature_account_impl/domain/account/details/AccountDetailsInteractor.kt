package io.novafoundation.nova.feature_account_impl.domain.account.details

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.entropy
import io.novafoundation.nova.common.data.secrets.v2.getAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.seed
import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.domain.model.hasChainAccountIn
import io.novafoundation.nova.feature_account_api.presenatation.account.add.SecretType
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountInChain.From
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
        val chains = chainRegistry.currentChains.first()

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
            .sortedBy { it.hasChainAccount }
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
}

private val From.ordering
    get() = when (this) {
        From.CHAIN_ACCOUNT -> 0
        From.META_ACCOUNT -> 1
    }
