package jp.co.soramitsu.feature_account_impl.domain.account.details

import jp.co.soramitsu.common.data.secrets.v2.SecretStoreV2
import jp.co.soramitsu.common.data.secrets.v2.entropy
import jp.co.soramitsu.common.data.secrets.v2.getAccountSecrets
import jp.co.soramitsu.common.data.secrets.v2.seed
import jp.co.soramitsu.common.list.GroupedList
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountRepository
import jp.co.soramitsu.feature_account_api.domain.model.MetaAccount
import jp.co.soramitsu.feature_account_api.domain.model.accountIdIn
import jp.co.soramitsu.feature_account_api.domain.model.addressIn
import jp.co.soramitsu.feature_account_api.domain.model.hasChainAccountIn
import jp.co.soramitsu.feature_account_impl.domain.account.details.AccountInChain.From
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain
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
            .groupBy(AccountInChain::from)
            .toSortedMap(compareBy(From::ordering))
    }

    suspend fun availableExportTypes(
        metaAccount: MetaAccount,
        chain: Chain,
    ): List<AvailableExportType> = withContext(Dispatchers.Default) {
        val accountId = metaAccount.accountIdIn(chain) ?: return@withContext emptyList()

        val secrets = secretStoreV2.getAccountSecrets(metaAccount.id, accountId)

        listOfNotNull(
            AvailableExportType.MNEMONC.takeIf { secrets.entropy() != null },
            AvailableExportType.SEED.takeIf { secrets.seed() != null },
            AvailableExportType.JSON // always available
        )
    }
}

private val From.ordering
    get() = when (this) {
        From.CHAIN_ACCOUNT -> 0
        From.META_ACCOUNT -> 1
    }
