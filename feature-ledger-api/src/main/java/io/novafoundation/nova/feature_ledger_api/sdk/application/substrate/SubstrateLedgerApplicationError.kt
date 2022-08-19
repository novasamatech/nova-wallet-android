package io.novafoundation.nova.feature_ledger_api.sdk.application.substrate

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

sealed class SubstrateLedgerApplicationError: Exception() {

    class UnsupportedApp(val chainId: ChainId): SubstrateLedgerApplicationError()

    class Response(val response: LedgerApplicationResponse): SubstrateLedgerApplicationError()
}
