package io.novafoundation.nova.feature_account_impl.domain.manualBackup

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.SECRETS
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.WATCH_ONLY
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.PARITY_SIGNER
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.LEDGER
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.POLKADOT_VAULT
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.PROXIED
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.hasAccountIn
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MetaAccountChains(
    val metaAccount: MetaAccount,
    val defaultChains: List<Chain>,
    val customChains: List<Chain>
)

interface ManualBackupInteractor {

    suspend fun getBackupableMetaAccounts(): List<MetaAccount>

    suspend fun getMetaAccount(id: Long): MetaAccount

    fun sortedMetaAccountChains(id: Long): Flow<MetaAccountChains>
}

class RealManualBackupInteractor(
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry
) : ManualBackupInteractor {

    override suspend fun getBackupableMetaAccounts(): List<MetaAccount> {
        return accountRepository.getActiveMetaAccounts()
            .filter { it.isBackupable() }
    }

    override suspend fun getMetaAccount(id: Long): MetaAccount {
        return accountRepository.getMetaAccount(id)
    }

    override fun sortedMetaAccountChains(id: Long): Flow<MetaAccountChains> {
        return chainRegistry.currentChains
            .map { chains ->
                val metaAccount = accountRepository.getMetaAccount(id)
                val sortedChains = chains.toSortedSet(Chain.defaultComparatorFrom { it })
                MetaAccountChains(
                    metaAccount,
                    sortedChains.filter { it.id !in metaAccount.chainAccounts.keys && metaAccount.hasAccountIn(it) },
                    sortedChains.filter { it.id in metaAccount.chainAccounts.keys },
                )
            }
    }

    private fun MetaAccount.isBackupable(): Boolean {
        return when (type) {
            SECRETS -> true

            WATCH_ONLY,
            PARITY_SIGNER,
            LEDGER,
            POLKADOT_VAULT,
            PROXIED -> false
        }
    }
}
