package io.novafoundation.nova.feature_ledger_api.sdk.application.substrate

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

sealed class SubstrateLedgerApplicationError(message: String) : Exception(message) {

    class UnsupportedApp(val chainId: ChainId) : SubstrateLedgerApplicationError("Unsupported app for chainId: $chainId")

    class Response(val response: LedgerApplicationResponse, val errorMessage: String?) :
        SubstrateLedgerApplicationError("Application error: $response")
}
