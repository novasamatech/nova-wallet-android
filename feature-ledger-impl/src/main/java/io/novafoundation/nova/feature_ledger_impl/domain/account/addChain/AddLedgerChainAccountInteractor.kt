package io.novafoundation.nova.feature_ledger_impl.domain.account.addChain

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.LedgerAddAccountRepository
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount

interface AddLedgerChainAccountInteractor {

    suspend fun addChainAccount(metaId: Long, chainId: String, account: LedgerSubstrateAccount): Result<Unit>
}

class RealAddLedgerChainAccountInteractor(
    private val ledgerAddAccountRepository: LedgerAddAccountRepository
) : AddLedgerChainAccountInteractor {

    override suspend fun addChainAccount(metaId: Long, chainId: String, account: LedgerSubstrateAccount): Result<Unit> = kotlin.runCatching {
        ledgerAddAccountRepository.addAccount(
            LedgerAddAccountRepository.Payload.ChainAccount(
                metaId,
                chainId,
                account
            )
        )
    }
}
