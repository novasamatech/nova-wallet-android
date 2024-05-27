package io.novafoundation.nova.feature_account_impl.data.cloudBackup

import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_impl.data.mappers.mapMetaAccountTypeFromLocal

fun MetaAccountLocal.Type.isBackupable(): Boolean {
    return mapMetaAccountTypeFromLocal(this).isBackupable()
}

fun LightMetaAccount.Type.isBackupable(): Boolean {
    return when (this) {
        LightMetaAccount.Type.SECRETS,
        LightMetaAccount.Type.WATCH_ONLY,
        LightMetaAccount.Type.PARITY_SIGNER,
        LightMetaAccount.Type.LEDGER,
        LightMetaAccount.Type.LEDGER_LEGACY,
        LightMetaAccount.Type.POLKADOT_VAULT -> true

        LightMetaAccount.Type.PROXIED -> false
    }
}
