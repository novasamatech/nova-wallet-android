package io.novafoundation.nova.feature_account_api.presenatation.common

import androidx.annotation.StringRes
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount

@StringRes
fun LightMetaAccount.Type.mapMetaAccountTypeToNameRes(): Int {
    return when (this) {
        LightMetaAccount.Type.SECRETS -> R.string.account_secrets
        LightMetaAccount.Type.WATCH_ONLY -> R.string.account_watch_only

        LightMetaAccount.Type.PARITY_SIGNER -> R.string.account_parity_signer
        LightMetaAccount.Type.POLKADOT_VAULT -> R.string.account_polkadot_vault

        LightMetaAccount.Type.LEDGER_LEGACY,
        LightMetaAccount.Type.LEDGER -> R.string.common_ledger

        LightMetaAccount.Type.PROXIED -> R.string.account_proxieds

        LightMetaAccount.Type.MULTISIG -> R.string.account_multisig_group_label
    }
}
