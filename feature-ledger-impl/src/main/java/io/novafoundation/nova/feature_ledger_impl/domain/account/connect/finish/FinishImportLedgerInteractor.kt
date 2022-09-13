package io.novafoundation.nova.feature_ledger_impl.domain.account.connect.finish

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_impl.data.repository.LedgerRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface FinishImportLedgerInteractor {

    suspend fun createWallet(
        name: String,
        ledgerChainAccounts: Map<ChainId, LedgerSubstrateAccount>,
    ): Result<Unit>
}

class RealFinishImportLedgerInteractor(
    private val repository: LedgerRepository,
    private val accountRepository: AccountRepository,
) : FinishImportLedgerInteractor {

    override suspend fun createWallet(name: String, ledgerChainAccounts: Map<ChainId, LedgerSubstrateAccount>) = runCatching {
        val metaId = repository.insertLedgerMetaAccount(name, ledgerChainAccounts)

        accountRepository.selectMetaAccount(metaId)
    }
}
