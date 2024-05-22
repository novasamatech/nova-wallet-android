package io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountRepository
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface LegacyLedgerAddAccountRepository : AddAccountRepository<LegacyLedgerAddAccountRepository.Payload> {

    sealed interface Payload {
        class MetaAccount(
            val name: String,
            val ledgerChainAccounts: Map<ChainId, LedgerSubstrateAccount>
        ) : Payload

        class ChainAccount(
            val metaId: Long,
            val chainId: ChainId,
            val ledgerChainAccount: LedgerSubstrateAccount
        ) : Payload
    }
}
