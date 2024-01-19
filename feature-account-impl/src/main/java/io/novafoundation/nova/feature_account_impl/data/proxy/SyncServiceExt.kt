package io.novafoundation.nova.feature_account_impl.data.proxy

import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

fun MetaAccount.isAllowedToSyncProxy(): Boolean {
    return when (type) {
        LightMetaAccount.Type.SECRETS,
        LightMetaAccount.Type.PARITY_SIGNER,
        LightMetaAccount.Type.LEDGER,
        LightMetaAccount.Type.POLKADOT_VAULT,
        LightMetaAccount.Type.WATCH_ONLY -> true

        LightMetaAccount.Type.PROXIED -> false
    }
}
