package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.data.signer.isImmediate
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

class BittensorImmediateExecution(
    private val signerProvider: SignerProvider
) {

    // This is an execution-timing gate, not signing authorization. Submission still uses
    // the normal signer hierarchy and its permission checks for the explicit wallet.
    suspend fun isSupported(wallet: MetaAccount): Boolean {
        return signerProvider.rootSignerFor(wallet).callExecutionType().isImmediate()
    }

    suspend fun requireSupported(wallet: MetaAccount) {
        require(isSupported(wallet)) { "Bittensor operations require immediate execution" }
    }
}
