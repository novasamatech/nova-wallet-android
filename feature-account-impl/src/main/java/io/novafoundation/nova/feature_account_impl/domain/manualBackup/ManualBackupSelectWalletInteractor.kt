package io.novafoundation.nova.feature_account_impl.domain.manualBackup

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.LEDGER
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.LEDGER_LEGACY
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.MULTISIG
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.PARITY_SIGNER
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.POLKADOT_VAULT
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.PROXIED
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.SECRETS
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.WATCH_ONLY
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

interface ManualBackupSelectWalletInteractor {

    suspend fun getBackupableMetaAccounts(): List<MetaAccount>

    suspend fun getMetaAccount(id: Long): MetaAccount
}

class RealManualBackupSelectWalletInteractor(
    private val accountRepository: AccountRepository
) : ManualBackupSelectWalletInteractor {

    override suspend fun getBackupableMetaAccounts(): List<MetaAccount> {
        return accountRepository.getActiveMetaAccounts()
            .filter { it.canBackupManually() }
    }

    override suspend fun getMetaAccount(id: Long): MetaAccount {
        return accountRepository.getMetaAccount(id)
    }

    private fun MetaAccount.canBackupManually(): Boolean {
        return when (type) {
            SECRETS -> true

            WATCH_ONLY,
            PARITY_SIGNER,
            LEDGER,
            LEDGER_LEGACY,
            POLKADOT_VAULT,
            MULTISIG,
            PROXIED -> false
        }
    }
}
