package io.novafoundation.nova.feature_account_impl.domain.manualBackup

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
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

interface ManualBackupSelectAccountInteractor {

    fun sortedMetaAccountChains(id: Long): Flow<MetaAccountChains>
}

class RealManualBackupSelectAccountInteractor(
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry
) : ManualBackupSelectAccountInteractor {

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
}
