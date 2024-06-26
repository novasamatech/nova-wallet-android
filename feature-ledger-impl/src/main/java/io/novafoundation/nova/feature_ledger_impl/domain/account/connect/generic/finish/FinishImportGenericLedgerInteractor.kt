package io.novafoundation.nova.feature_ledger_impl.domain.account.connect.generic.finish

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.addAccountWithSingleChange
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.GenericLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount

interface FinishImportGenericLedgerInteractor {

    suspend fun createWallet(name: String, universalAccount: LedgerSubstrateAccount): Result<Unit>
}

class RealFinishImportGenericLedgerInteractor(
    private val genericLedgerAddAccountRepository: GenericLedgerAddAccountRepository,
    private val accountRepository: AccountRepository,
) : FinishImportGenericLedgerInteractor {

    override suspend fun createWallet(name: String, universalAccount: LedgerSubstrateAccount) = runCatching {
        val payload = GenericLedgerAddAccountRepository.Payload.NewWallet(
            name = name,
            universalAccount = universalAccount
        )

        val addAccountResult = genericLedgerAddAccountRepository.addAccountWithSingleChange(payload)

        accountRepository.selectMetaAccount(addAccountResult.metaId)
    }
}
