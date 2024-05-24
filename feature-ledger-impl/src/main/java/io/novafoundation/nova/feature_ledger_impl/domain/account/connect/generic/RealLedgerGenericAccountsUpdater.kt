package io.novafoundation.nova.feature_ledger_impl.domain.account.connect.generic

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.GenericLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_ledger_api.domain.generic.LedgerGenericAccountsUpdater
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import kotlinx.coroutines.flow.map

internal class RealLedgerGenericAccountsUpdater(
    private val ledgerMigrationTracker: LedgerMigrationTracker,
    private val metaAccountRepository: AccountRepository,
    private val genericLedgerAddAccountRepository: GenericLedgerAddAccountRepository,
): LedgerGenericAccountsUpdater {

    override fun updateAvailableGenericAccounts() = ledgerMigrationTracker.supportedChainIdsByGenericAppFlow().map { chainIds ->
        runCatching {
            metaAccountRepository.getMetaAccountIdsByType(LightMetaAccount.Type.LEDGER).onEach { metaId ->
                val payload = GenericLedgerAddAccountRepository.Payload.AddMissingChainAccounts(
                    metaId = metaId,
                    allAvailableChainIds = chainIds
                )

                genericLedgerAddAccountRepository.addAccount(payload)
            }
        }

        Unit
    }
}
