package io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountRepository
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount

interface GenericLedgerAddAccountRepository : AddAccountRepository<GenericLedgerAddAccountRepository.Payload> {

    sealed interface Payload {

        class NewWallet(
            val name: String,
            val universalAccount: LedgerSubstrateAccount,
        ) : Payload
    }
}
