package io.novafoundation.nova.feature_ledger_impl.domain.account.connect.generic.finish

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.GenericLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker

interface FinishImportGenericLedgerInteractor {

    suspend fun createWallet(name: String, universalAccount: LedgerSubstrateAccount): Result<Unit>
}

class RealFinishImportGenericLedgerInteractor(
    private val genericLedgerAddAccountRepository: GenericLedgerAddAccountRepository,
    private val accountRepository: AccountRepository,
    private val ledgerMigrationTracker: LedgerMigrationTracker
) : FinishImportGenericLedgerInteractor {

    override suspend fun createWallet(name: String, universalAccount: LedgerSubstrateAccount) = runCatching {
        val availableChains = ledgerMigrationTracker.supportedChainsByGenericApp()
        val payload = GenericLedgerAddAccountRepository.Payload.NewWallet(
            name = name,
            availableChains = availableChains,
            universalAccount = universalAccount
        )

        val addAccountResult = genericLedgerAddAccountRepository.addAccount(payload)

        accountRepository.selectMetaAccount(addAccountResult.metaId)
    }
}
