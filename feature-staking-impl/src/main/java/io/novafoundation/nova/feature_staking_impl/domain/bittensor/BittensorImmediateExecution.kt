package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.data.signer.isImmediate
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

enum class BittensorWalletSupport {
    SUPPORTED, PROXIED_NOT_SUPPORTED, DELAYED_EXECUTION
}

class BittensorImmediateExecution(
    private val signerProvider: SignerProvider
) {

    // A proxied wallet is refused by type, a multisig only when its calls execute with a delay.
    // This is not signing authorization: submission still uses the normal signer hierarchy.
    suspend fun support(wallet: MetaAccount): BittensorWalletSupport = when (wallet.type) {
        LightMetaAccount.Type.SECRETS,
        LightMetaAccount.Type.WATCH_ONLY,
        LightMetaAccount.Type.PARITY_SIGNER,
        LightMetaAccount.Type.POLKADOT_VAULT,
        LightMetaAccount.Type.LEDGER_LEGACY,
        LightMetaAccount.Type.LEDGER -> BittensorWalletSupport.SUPPORTED

        LightMetaAccount.Type.PROXIED -> BittensorWalletSupport.PROXIED_NOT_SUPPORTED

        LightMetaAccount.Type.MULTISIG -> if (signerProvider.rootSignerFor(wallet).callExecutionType().isImmediate()) {
            BittensorWalletSupport.SUPPORTED
        } else {
            BittensorWalletSupport.DELAYED_EXECUTION
        }
    }

    suspend fun isSupported(wallet: MetaAccount): Boolean {
        return support(wallet) == BittensorWalletSupport.SUPPORTED
    }

    suspend fun requireSupported(wallet: MetaAccount) {
        require(isSupported(wallet)) { "Bittensor operations are not supported for this wallet" }
    }
}
