package io.novafoundation.nova.feature_ledger_impl.domain.account.addChain.generic

import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.utils.coerceToUnit
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.GenericLedgerAddAccountRepository
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerEvmAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface AddEvmAccountToGenericLedgerInteractor {

    suspend fun addEvmAccount(metaId: Long, account: LedgerEvmAccount): Result<Unit>
}

@ScreenScope
class RealAddEvmAccountToGenericLedgerInteractor @Inject constructor(
    private val genericLedgerAddAccountRepository: GenericLedgerAddAccountRepository
): AddEvmAccountToGenericLedgerInteractor {

    override suspend fun addEvmAccount(metaId: Long, account: LedgerEvmAccount): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = GenericLedgerAddAccountRepository.Payload.AddEvmAccount(metaId, account)
            genericLedgerAddAccountRepository.addAccount(payload)
        }.coerceToUnit()
    }
}
