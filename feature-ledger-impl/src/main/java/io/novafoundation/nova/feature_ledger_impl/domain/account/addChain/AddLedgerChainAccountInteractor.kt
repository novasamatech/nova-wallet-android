package io.novafoundation.nova.feature_ledger_impl.domain.account.addChain

import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_impl.data.repository.LedgerRepository

interface AddLedgerChainAccountInteractor {

    suspend fun addChainAccount(metaId: Long, chainId: String, account: LedgerSubstrateAccount): Result<Unit>
}

class RealAddLedgerChainAccountInteractor(
    private val ledgerRepository: LedgerRepository,
): AddLedgerChainAccountInteractor {

    override suspend fun addChainAccount(metaId: Long, chainId: String, account: LedgerSubstrateAccount): Result<Unit> = kotlin.runCatching {
        ledgerRepository.insertLedgerChainAccount(metaId, chainId, account)
    }
}
