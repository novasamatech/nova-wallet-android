package io.novafoundation.nova.feature_ledger_impl.domain.account.connect.finish

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.LedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface FinishImportLedgerInteractor {

    suspend fun createWallet(
        name: String,
        ledgerChainAccounts: Map<ChainId, LedgerSubstrateAccount>,
    ): Result<Unit>
}

class RealFinishImportLedgerInteractor(
    private val ledgerAddAccountRepository: LedgerAddAccountRepository,
    private val accountRepository: AccountRepository,
) : FinishImportLedgerInteractor {

    override suspend fun createWallet(name: String, ledgerChainAccounts: Map<ChainId, LedgerSubstrateAccount>) = runCatching {
        val metaId = ledgerAddAccountRepository.addAccount(
            LedgerAddAccountRepository.Payload.MetaAccount(
                name,
                ledgerChainAccounts
            )
        )

        accountRepository.selectMetaAccount(metaId)
    }
}
