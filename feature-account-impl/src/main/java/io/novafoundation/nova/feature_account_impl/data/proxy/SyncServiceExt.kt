package io.novafoundation.nova.feature_account_impl.data.proxy

import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

fun MetaAccount.isAllowedToSyncProxy(shouldSyncWatchOnly: Boolean): Boolean {
    return when (type) {
        LightMetaAccount.Type.SECRETS,
        LightMetaAccount.Type.PARITY_SIGNER,
        LightMetaAccount.Type.LEDGER_LEGACY,
        LightMetaAccount.Type.LEDGER,
        LightMetaAccount.Type.POLKADOT_VAULT,
        LightMetaAccount.Type.MULTISIG -> true

        LightMetaAccount.Type.WATCH_ONLY -> shouldSyncWatchOnly

        LightMetaAccount.Type.PROXIED -> false
    }
}
