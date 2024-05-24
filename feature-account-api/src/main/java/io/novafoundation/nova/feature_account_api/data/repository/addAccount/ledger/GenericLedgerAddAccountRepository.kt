package io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountRepository
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface GenericLedgerAddAccountRepository : AddAccountRepository<GenericLedgerAddAccountRepository.Payload> {

    sealed interface Payload {

        class NewWallet(
            val name: String,
            val availableChains: Collection<Chain>,
            val universalAccount: LedgerSubstrateAccount,
        ) : Payload

        // TODO next PR - all new chains supported by generic ledger automatically get their chain accounts
//        class AddMissingChainAccounts(
//            val metaId: Long,
//            val availableChainAccounts: Map<ChainId, LedgerSubstrateAccount>
//        )
    }
}
