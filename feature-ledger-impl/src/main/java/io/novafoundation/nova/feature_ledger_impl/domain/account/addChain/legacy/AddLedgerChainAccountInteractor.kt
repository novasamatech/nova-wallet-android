package io.novafoundation.nova.feature_ledger_impl.domain.account.addChain.legacy

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.LegacyLedgerAddAccountRepository
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount

interface AddLedgerChainAccountInteractor {

    suspend fun addChainAccount(metaId: Long, chainId: String, account: LedgerSubstrateAccount): Result<Unit>
}

class RealAddLedgerChainAccountInteractor(
    private val legacyLedgerAddAccountRepository: LegacyLedgerAddAccountRepository
) : AddLedgerChainAccountInteractor {

    override suspend fun addChainAccount(metaId: Long, chainId: String, account: LedgerSubstrateAccount): Result<Unit> = kotlin.runCatching {
        legacyLedgerAddAccountRepository.addAccount(
            LegacyLedgerAddAccountRepository.Payload.ChainAccount(
                metaId,
                chainId,
                account
            )
        )
    }
}
