package io.novafoundation.nova.feature_account_api.domain.model

fun metaAccountTypeComparator() = compareBy<LightMetaAccount.Type> {
    when (it) {
        LightMetaAccount.Type.SECRETS -> 0
        LightMetaAccount.Type.POLKADOT_VAULT -> 1
        LightMetaAccount.Type.PARITY_SIGNER -> 2
        LightMetaAccount.Type.LEDGER -> 3
        LightMetaAccount.Type.LEDGER_LEGACY -> 4
        LightMetaAccount.Type.PROXIED -> 5
        LightMetaAccount.Type.WATCH_ONLY -> 6
    }
}
