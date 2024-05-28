package io.novafoundation.nova.feature_account_impl.data.cloudBackup

import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount

fun MetaAccountLocal.Type.isBackupable(): Boolean {
    return when (this) {
        MetaAccountLocal.Type.SECRETS,
        MetaAccountLocal.Type.WATCH_ONLY,
        MetaAccountLocal.Type.PARITY_SIGNER,
        MetaAccountLocal.Type.LEDGER,
        MetaAccountLocal.Type.POLKADOT_VAULT -> true

        MetaAccountLocal.Type.PROXIED -> false
    }
}

fun LightMetaAccount.Type.isBackupable(): Boolean {
    return when (this) {
        LightMetaAccount.Type.SECRETS,
        LightMetaAccount.Type.WATCH_ONLY,
        LightMetaAccount.Type.PARITY_SIGNER,
        LightMetaAccount.Type.LEDGER,
        LightMetaAccount.Type.POLKADOT_VAULT -> true

        LightMetaAccount.Type.PROXIED -> false
    }
}
