package io.novafoundation.nova.feature_account_impl.domain.manualBackup

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.SECRETS
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.WATCH_ONLY
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.PARITY_SIGNER
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.LEDGER
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.POLKADOT_VAULT
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.PROXIED
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

interface ManualBackupInteractor {
    suspend fun getBackupableMetaAccounts(): List<MetaAccount>
}

class RealManualBackupInteractor(
    private val accountRepository: AccountRepository
) : ManualBackupInteractor {

    override suspend fun getBackupableMetaAccounts(): List<MetaAccount> {
        return accountRepository.getActiveMetaAccounts()
            .filter { it.isBackupable() }
    }

    private fun MetaAccount.isBackupable(): Boolean {
        return when (type) {
            SECRETS -> true

            WATCH_ONLY,
            PARITY_SIGNER,
            LEDGER,
            POLKADOT_VAULT,
            PROXIED -> false
        }
    }
}
