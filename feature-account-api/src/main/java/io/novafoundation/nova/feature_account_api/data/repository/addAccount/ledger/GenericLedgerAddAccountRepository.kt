package io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountRepository
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface GenericLedgerAddAccountRepository : AddAccountRepository<GenericLedgerAddAccountRepository.Payload> {

    sealed interface Payload {

        class NewWallet(
            val name: String,
            val availableChains: Collection<Chain>,
            val universalAccount: LedgerSubstrateAccount,
        ) : Payload

        // TODO after merging cloud backup changes to our branch we can do better and support multiple meta account handling
        // To avoid multiple updates over the MetaAccountChangesEventBus
        class AddMissingChainAccounts(
            val metaId: Long,
            val allAvailableChainIds: Collection<ChainId>,
        ): Payload
    }
}
