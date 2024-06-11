package io.novafoundation.nova.feature_ledger_api.data.repository

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

object LedgerDerivationPath {

    private const val LEDGER_DERIVATION_PATH_KEY = "LedgerChainAccount.derivationPath"

    fun derivationPathSecretKey(chainId: ChainId): String {
        return "$LEDGER_DERIVATION_PATH_KEY.$chainId"
    }

    fun genericDerivationPathSecretKey(): String {
        return "$LEDGER_DERIVATION_PATH_KEY.Generic"
    }
}
